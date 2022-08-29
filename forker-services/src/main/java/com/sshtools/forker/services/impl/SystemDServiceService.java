package com.sshtools.forker.services.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.interfaces.Properties.PropertiesChanged;

import com.sshtools.forker.client.OSCommand;
import com.sshtools.forker.common.Util.NullOutputStream;
import com.sshtools.forker.services.AbstractService;
import com.sshtools.forker.services.Service;
import com.sshtools.forker.services.ServiceService;
import com.sshtools.forker.services.ServicesContext;

import de.thjom.java.systemd.Manager;
import de.thjom.java.systemd.Systemd;
import de.thjom.java.systemd.Unit;
import de.thjom.java.systemd.Unit.Mode;
import de.thjom.java.systemd.types.UnitFileType;
import de.thjom.java.systemd.types.UnitType;

public class SystemDServiceService extends AbstractServiceService implements ServiceService {

	private final class UnitTypeService extends AbststractUnitFileService {
		private final UnitType uf;

		private UnitTypeService(String name, UnitType uf) {
			super(name);
			this.uf = uf;
		}

		@Override
		String getState() {
			return uf.getActiveState();
		}

	}	

	private abstract class AbststractUnitFileService extends SystemDService {

		private AbststractUnitFileService(String name) {
			super(name);
		}

		@Override
		Unit getUnit() throws IOException {
			if (unit == null) {
				try {
					unit = (de.thjom.java.systemd.Service) systemd.getManager().getUnit(toServiceName(this));
				} catch (Exception e) {
					throw new IOException(String.format("Could not get service %s.", getNativeName()), e);
				}
			}
			return unit;
		}

		de.thjom.java.systemd.Service unit;
	}

	private final class UnitFileTypeService extends AbststractUnitFileService {
		private final UnitFileType uf;

		private UnitFileTypeService(String name, UnitFileType uf) {
			super(name);
			this.uf = uf;
		}

		@Override
		String getState() {
			return uf.getStatus();
		}

	}

	private abstract class SystemDService extends AbstractService {

		SystemDService(String name) {
			super(name);
		}

		@Override
		public Status getStatus() {
			String st = getState();
			if ("activating".equals(st)) {
				return Status.STARTING;
			} else if ("deactivating".equals(st)) {
				return Status.STOPPING;
			} else if ("active".equals(st)) {
				return Status.STARTED;
			} else if ("inactive".equals(st)) {
				return Status.STOPPED;
			} else if ("failed".equals(st)) {
				return Status.STOPPED;
			} else {
				LOG.fine(String.format("Unknown systemd state %s", st));
				return Status.STOPPED;
			}
		}

		abstract String getState();

		abstract Unit getUnit() throws IOException;

		public void stop() throws IOException {
			getUnit().stop(Mode.REPLACE);
		}

		public void start() throws IOException {
			getUnit().start(Mode.REPLACE);

		}

		public void restart() throws IOException {
			getUnit().restart(Mode.REPLACE);
		}
	}

	final static Logger LOG = Logger.getLogger(SystemDServiceService.class.getName());

	private Systemd systemd;
	private DBusConnection conn;

	@Override
	public List<Service> getServices() throws IOException {
		try {
			List<UnitType> units = systemd.getManager().listUnits();
			List<Service> l = new ArrayList<>(units.size());
			Set<String> s = new HashSet<>();
			for (UnitType uf : units) {
				if (uf.isService()) {
					Service usrv = unitToService(uf);
					s.add(usrv.getNativeName());
					l.add(usrv);
				}
			}
			List<UnitFileType> unitFileTypes = systemd.getManager().listUnitFiles();
			for (UnitFileType uf : unitFileTypes) {
				if (uf.isService()) {
					Service usrv = unitToService(uf);
					if (!s.contains(usrv.getNativeName()))
						l.add(usrv);
				}
			}
			return l;
		} catch (DBusException dbe) {
			throw new IOException("Failed to get systemd services.", dbe);
		}
	}

	private Service unitToService(final UnitType uf) {
		return new UnitTypeService(getBaseName(uf.getUnitName()), uf);
	}

	private String getBaseName(String name) {
		int idx = name.indexOf('/');
		if(idx != -1) {
			name = name.substring(idx + 1);
		}
		idx = name.lastIndexOf('.');
		if(idx != -1)
			name = name.substring(0, idx);
		return name;
	}

	private Service unitToService(final UnitFileType uf) {
		return new UnitFileTypeService(getBaseName(uf.getPath()), uf);
	}

	@Override
	public void configure(ServicesContext app) {

		try {
			systemd = Systemd.get();
			final Manager manager = systemd.getManager();
			manager.subscribe();
			LOG.info("Connecting to System DBus");
			conn = DBusConnectionBuilder.forSystemBus().withShared(false).build();
			conn.addSigHandler(PropertiesChanged.class, new DBusSigHandler<PropertiesChanged>() {
				@Override
				public void handle(PropertiesChanged sig) {
					if (sig.getInterface().equals("org.freedesktop.systemd1.Service")) {
						try {
							Properties props = conn.getRemoteObject(Systemd.SERVICE_NAME, sig.getPath(),
									Properties.class);
							List<String> names = props.Get("org.freedesktop.systemd1.Unit", "Names");
							for (String n : names) {
								final de.thjom.java.systemd.Service unit = (de.thjom.java.systemd.Service) systemd
										.getManager().getUnit(n);
								fireStateChange(new SystemDService(getBaseName(n)) {
									@Override
									String getState() {
										return unit.getActiveState();
									}

									@Override
									Unit getUnit() {
										return unit;
									}

								});
							}
						} catch (DBusException e) {
							LOG.log(Level.SEVERE, "Failed to get unit for property change.", e);
						}
					}
				}
			});

		} catch (DBusException e) {
			throw new IllegalStateException("Could not connect to SystemD");
		}

	}

	@Override
	public void restartService(Service service) throws Exception {
		((SystemDService) service).restart();
	}

	@Override
	public void startService(Service service) throws Exception {
		((SystemDService) service).start();
	}

	@Override
	public void stopService(Service service) throws Exception {
		((SystemDService) service).stop();
	}

	@Override
	public Service getService(String name) throws IOException {
		try {
			final de.thjom.java.systemd.Service unit = (de.thjom.java.systemd.Service) systemd.getManager()
					.getUnit(name + ".service");
			return new SystemDService(name) {
				@Override
				String getState() {
					return unit.getActiveState();
				}

				@Override
				Unit getUnit() {
					return unit;
				}

			};
		} catch (DBusException dbe) {
			throw new IOException("Failed to get systemd service.", dbe);
		}
	}

	@Override
	public void setStartOnBoot(Service service, boolean startOnBoot) throws Exception {
		OSCommand.elevate();
		try {
			OSCommand.runCommand(new NullOutputStream(),
					Arrays.asList("systemctl", startOnBoot ? "enable" : "disable", toServiceName(service)));
		} finally {
			OSCommand.restrict();
		}
	}

	@Override
	public boolean isStartOnBoot(Service service) throws Exception {
		return OSCommand.runCommand(null, System.out, "systemctl", "is-enabled", toServiceName(service)) == 0;
	}

	protected String toServiceName(Service service) {
		return service.getNativeName() + ".service";
	}
}
