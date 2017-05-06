
================================================

This software is distributed under GPLv3 license. 
A copy is included

================================================

Description
------

This is a game for learning coordination protocols in a microgrid scenario. There are solar panels to switch on or off. There are buildings consuming energy. The game requires keeping generation as close as possible to the consumption. 

The game has three possible setups (parameter "scenario" in config.json file). The role of the students in each scenario is determined on a first come first serve basis as they reach the "/sg/pages/panel" URL:
- fulloperational. All students have all the information (metering+billing+weather) and access to control of all panels.
- fullsolarbyperson. All students have all the information (metering+billing+weather) but access to a single solar panel
- centralized. One person plays the "SCADA" role and has all the information (metering+billing+weather) but no control access to any panel. The rest of students have acces to the weather and a control button for a single panel.

Additional thoughts:
- Students should play some training games
- the game does not inform on the status of the solar panel. It tells which was the last order issued by one player
- organize teams and set up as many servers (to different ports) as required. Each microgrid can have as many players as solar panels are availables (centralized version, fullsolarbyperson) or as many as required by the organizer (fulloperational). 
- in the centralized version, the SCADA role can use contract-net to determine who has the right solar panel to activate
- You cannot run two servers on the same folder. Copy the installation folder to another place and modify the config.json to set a different port for each instance.
- in the fulloperational, it is challenging to determine who can act at a given time. There maybe conflicting orders if sent to the same solar panel



Requirements
--------

The following software is to be installed:

- maven 3.0 or superior
- JDK 1.8 or superior

Check it works running in a console "javac" and "mvn". If "command not found" error appears, then it is not well installed.

Instructions:
------

1. edit config.json to set the "host" and "adminip". By default, these are set to "localhost". If you want to offer the service OUTSIDE your computer, set this to the static/dynamic IP of the host. Think twice about the "adminip". It is valid to have a static ip in "host" and "localhost" in "adminip". This setup ensures that only requests coming from localhost will be attended and other computers will be excluded. This is only working in setups where the server and the admin are in the same computer. Run "ifconfig" in linux or "ipconfig" in windows to get this IP. Set a suitable port. If no program is using it, port 80 or 8080 is an option too. By default, the port is 60000. Set the operation mode (see description for more details).
2. launch the system with "sh launch.sh" or "launch.bat"
3. Copy the addresses you will see in the console. The "/sg/pages/panel" is to be allocated to the students. You can check progress with the admin pages "/sg/pages/admin" and "/sg/pages/screen".


If you want to see a representation of the microgrid

mvn clean compile exec:java -Dexec.mainClass="net.sf.sgsimulator.sgsrest.Viewer"

Credits
---------

SGSimulator-Rest by Jorge J. Gomez Sanz (@jjgomezs, jjgomez@ucm.es) and Rafael Pax (rpax@ucm.es)
SGSImulator (http://sgsimulator.sf.net) by Jorge J. Gomez Sanz, Nuria Cuartero-Soler, Sandra Garcia-Rodriguez

We acknowledge support from the project “Collaborative Ambient Assisted Living Design (ColoSAAL)” (TIN2014-57028-R ) funded by Spanish Ministry for Economy and Competitiveness; and MOSI-AGIL-CM (S2013/ICE-3019) co-funded by Madrid Government, EU Structural Funds FSE, and FEDER; and MIRED-CON project 

