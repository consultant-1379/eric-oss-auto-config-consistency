# Creating a scope file with all ManagedElement FDNs

Run the following commands from the scope-file directory in localScripts

1. kubectl exec -it -n restsim eric-oss-restsim-postgres-0 -- bash
2. psql -U restsim
3. \o /tmp/managed_elements.txt;
4. select uri from modb where mo='ManagedElement';
5. \o
6. \q
7. exit
8. kubectl cp -n restsim eric-oss-restsim-postgres-0:/tmp/managed_elements.txt ../tmp/managed_elements.txt
9. kubectl exec -n restsim eric-oss-restsim-postgres-0 -- bash -c 'rm /tmp/managed_elements.txt'

Use the list of managed element FDNs in managed_elements.txt to create your scope file for the k6 load tests.