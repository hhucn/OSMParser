# OSMParser
The main functions of this java program are:
- parse an OSM.xml file
- remove all edges and nodes not belonging to roads allowing vehicular traffic
- split all OSM ways with more than two nodes into seperate ways
- create directed edges for every original bidirectional way

The OSMParser is used as the first preprocessing step to generate the TBUS simulation database


##Compile
javac OSMParser.jav

##Run
java OSMParser
