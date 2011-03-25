Testbed Runtime
===============

What is Testbed Runtime?
--------------------------
 
Testbed Runtime is a set of programs that together form a wireless sensor networks
testbed infrastructure. It implements the APIs defined by the european research project
[WISEBED][wisebed], namely RS (Reservation System), SNAA (Sensor Network Authentication and
Authorization) and iWSN (Wireless Sensor Network API).

Documentation and Issue Tracking
--------------------------------

All documentation and issue tracking is currently to be found on our [project trac][tr-trac]
installation. Please report bugs there. To get write access to trac please contact
Daniel Bimschas (bimschas -at- itm.uni-luebeck.de).

Mailing List
------------

There's a user mailing list (testbed-runtime-users@itm.uni-luebeck.de) you are free
to join by sending an e-mail to testbed-runtime-users-subscribe@itm.uni-luebeck.de, to
unsubscribe please send an e-mail to testbed-runtime-users-unsubscribe@itm.uni-luebeck.de.
You can also visit the mailing list archives at [4]. Community and developer support will
be preferably given through this list.

Binary Downloads
----------------

Please see the projects [trac][tr-trac] (wiki and ticket system) for binary downloads.

Building
--------

Testbed Runtime is based on the [Apache Maven][maven] build system. Clone the project and
simply run 'mvn package' or 'mvn install' for building.

Modules
-------

Testbed Runtime delivers a set of modules that can be independently used from each
other (as long as dependencies are met). A description of the individual modules here
is yet to be done, stay tuned. 

License
-------

The project is made open-source under the terms of the BSD license, was created and is
maintained by the Institute of Telematics, University of Luebeck, Germany.

[wisebed]:http://www.wisebed.eu/
[maven]:http://maven.apache.org/
[tr-trac]:https://www.itm.uni-luebeck.de/projects/testbed-runtime/
[tr-mailinglist]:http://www.itm.uni-luebeck.de/pipermail/testbed-runtime-users/
