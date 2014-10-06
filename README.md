splunk-db-connect
=================

Splunk Java Oracle Database Agent

(c) 2014 INFIGO IS d.o.o.
Written by Marijan Fofonjka <marijan.fofonjka@infigo.hr>

This agent allows you to iteratively retrieve data from Oracle databases. The agent retrieves the data and outputs it to standard output, allowing easy indexing with Splunk.
The agent collects data through a pre-defined SQL statement defined in the configuration file.

In order to configure the agent, first create a config file called input_config in the Encryptor directory. Then use the Encryptor to encrypt the configuration which will be subsequently read by the agent. This allows you to "obfuscate" the connection string used to access the database.

If the agent encounters an error, it will be logged into the oracle_database_agent.log file in its directory.

Requirements:
- Java 1.5.0 or newer
- Oracle 10g DB or newer

Config file parameters:

1. host (mandatory)

2. port (mandatory)

3. SID (mandatory)

4. username (optional) - if not used just enter: username=

5. password (optional) - if not used just ener: password=

6. columnDelimiter (mandatory) - delimiter that will be used by the agent. Usage of tab is recommended - enter literally

7. columnIDType (mandatory) - possible values: number, time or rowid depending on the follow tail option

8. columnNameID (mandatory) - name of column that will be used for iterative requests (number - integer, time - (TIMESTAMP) or rowid - Oracle ROWID)

9. columnNameIDWhere (mandatory) - name of the ID column that will be used in the WHERE SQL statement. Can be also entered as alias.ID

10. columnNameTime (optional) - column containing time that will be used by Splunk. If not used leave empty as columnNameTime=

11. checkTableRotation (mandatory) - can be true or false. Specifies if agent will check if the table has been rotated when using number or rowid as column for follow tail. The agent verifies if the table has been rotated by checking the largest ID of the record received in the previous iteration. If in the current iteration that record does not exist, the agent assumes that the table has been rotated and will read it from the beginning.

12. sqlSelect (mandatory) - SQL statement used to retrieve the data. Can span multiple lines and must contain $substitute$. The $substitute$ parameter is changed by the agent actively with the following command: columnNameIDWHERE > 'max_ID_previous_iteration'. If ROWID are used, the SQL statement must print the ROWID column and add "order by ROWID".


How to use:

1) Put config data into input_config
2) Optionally put the initial state into the "state" file (i.e. if you want the agent to start retrieving data form row 1000 put that into the "state" file).
3) Encrypt the config file: java -jar oracle_database_encryptor.jar
4) Enjoy.
