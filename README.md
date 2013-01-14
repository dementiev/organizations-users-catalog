organizations-users-catalog
===========================

Creates a directory of organizations in the Jira (sites that are running our software), creating a new user in the system must be assigned to the organization of the directory, while maintaining the user to add it to the groups that are in the organization. 
If you change the organization - converted by the groups.
Each organization must be established reference projects (from project Jira) showing only the version for each project (from the directory of the project releases).
When creating a query is automatically substituted by the organization and the user fills the "System Version" which is inserted information about the installed version of the project from the company directory.


Requirements to the Company Directory:
1) fields: name, address, phone, website, a list of groups Jira
2) the right to edit: the users in the group: jira-ref-admin
3) the right to view: jira-ref-view
4) Reference established projects, indicating the version for each project (from releases of the project)


Requirements for Manual:
1) fields: basic position, phone, mobile phone
2) the right to edit: the users in the group: jira-ref-admin
3) the right to view: jira-ref-view


 When creating / editing a user box, "Group", in which using the autocomplete select groups by name from the directory of groups.
Opportunity to create a query to obtain from reporter id category and filling custom field of type "Group"
Jira version 4.2