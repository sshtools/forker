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
import com.sshtools.forker.services.impl.systemd.ListUnitFilesStruct;
import com.sshtools.forker.services.impl.systemd.ListUnitsStruct;
import com.sshtools.forker.services.impl.systemd.Manager;
import com.sshtools.forker.services.impl.systemd.Unit;

public class SystemDServiceService extends AbstractServiceService implements ServiceService {

	private final class UnitTypeService extends AbststractUnitFileService {
		private final Unit uf;

		private UnitTypeService(String name, Unit uf) {
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
					unit = manager.GetUnit(toServiceName(this));
				} catch (Exception e) {
					throw new IOException(String.format("Could not get service %s.", getNativeName()), e);
				}
			}
			return unit;
		}

		Unit unit;
	}

	private final class UnitFileTypeService extends AbststractUnitFileService {
		private final ListUnitFilesStruct uf;

		private UnitFileTypeService(String name, ListUnitFilesStruct uf) {
			super(name);
			this.uf = uf;
		}

		@Override
		String getState() {
			return uf.getState();
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
			getUnit().Stop("replace");
		}

		public void start() throws IOException {
			getUnit().Start("replace");

		}

		public void restart() throws IOException {
			getUnit().Restart("replace");
		}
	}

	final static Logger LOG = Logger.getLogger(SystemDServiceService.class.getName());

	private DBusConnection conn;
	private Manager manager;

	@Override
	public List<Service> getServices() throws IOException {
		List<ListUnitsStruct> units = manager.ListUnits();
		List<Service> l = new ArrayList<>(units.size());
		Set<String> s = new HashSet<>();
		for (ListUnitsStruct uf : units) {
			Service usrv = unitToService(uf);
			s.add(usrv.getNativeName());
			l.add(usrv);
		}
		List<ListUnitFilesStruct> unitFileTypes = manager.ListUnitFiles();
		for (ListUnitFilesStruct uf : unitFileTypes) {
			Service usrv = unitToService(uf);
			if (!s.contains(usrv.getNativeName()))
				l.add(usrv);
		}
		return l;
	}

	private Service unitToService(final ListUnitsStruct uf) {
		return new UnitTypeService(getBaseName(uf.getName()), uf.getUnit());
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

	private Service unitToService(final ListUnitFilesStruct uf) {
		return new UnitFileTypeService(getBaseName(uf.getName()), uf);
	}

	@Override
	public void configure(ServicesContext app) {

		try {
			LOG.info("Connecting to System DBus");
			conn = DBusConnectionBuilder.forSystemBus().withShared(false).build();
			conn.addSigHandler(PropertiesChanged.class, new DBusSigHandler<PropertiesChanged>() {
				@Override
				public void handle(PropertiesChanged sig) {
					if (sig.getInterface().equals("org.freedesktop.systemd1.Service")) {
						try {
							Properties props = conn.getRemoteObject("org.freedesktop.systemd1", sig.getPath(),
									Properties.class);
							List<String> names = props.Get("org.freedesktop.systemd1.Unit", "Names");
							for (String n : names) {
								var unit = manager.GetUnit(n);
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
			
			manager = conn.getRemoteObject("org.freedesktop.systemd1", "/org/freedesktop/systemd1", Manager.class);

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
		var unit = manager.GetUnit(name + ".service");
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
