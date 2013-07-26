This bundle is used to extend the OSGi framework such that packages from Testbed Runtime and its third party
dependencies are exposed to the plugin. It uses the OSGi fragment bundle mechanism to enhance the "system.bundle" bundle
"Export-Package" header. Read more about the mechanism here:

  http://blog.meschberger.ch/2008/10/osgi-bundles-require-classes-from.html

The resulting bundle is copied to $TR_ROOT/common/src/main/resources/de/uniluebeck/itm/tr/common/plugins and then gets
installed by the PluginContainer instance upon bootstrapping the OSGi environment. This way it is assured that the
bundle is already installed before the org.apache.felix.fileinstall bundle starts any other TR plugins.