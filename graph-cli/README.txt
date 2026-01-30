graph-cli
You may need to chmod +x run.sh
=========

A simple command-line tool for interacting with Graph users.

--------------------------------------------------
Build Instructions
--------------------------------------------------

Build (dev):
 - mvn compile
 - mvn test
 - mvn -q exec:java

Run (recommended):
 - ./run.sh
 You may need to chmod +x run.sh


--------------------------------------------------
Supported operations
--------------------------------------------------

 - 'search' : find users by name, UPN, email, or object ID
 - 'delete' : delete a single matched user
 - 'invite' : invite an external user (guest)

--------------------------------------------------
Config Specifications
--------------------------------------------------

This tool requires two files:
 - config.ini : operation + behavior
 - auth.ini   : Azure AD credentials

Both files must exist and be filled out in order to run the application.
Recommended:  
 - $ cp example.auth auth.ini
 - $ cp example.config config.ini

--------------------------------------------------
config.ini
--------------------------------------------------

[operation]
mode = search | delete | invite

[search]
query = NAME | UPN | EMAIL | OBJECT_ID
maxResults = 25

[invite]
email = external user's email address
redirectUrl = URL redirected to after invitation is accepted
sendInvitationMessage = true | false

--------------------------------------------------
auth.ini
--------------------------------------------------

[auth]
tenantId     = Azure AD tenant ID (GUID)
clientId     = App registration client ID
clientSecret = App registration secret
scope        = https://graph.microsoft.com/.default

--------------------------------------------------
Additional Notes
--------------------------------------------------

Query will automatically detect ID, Email, Name, or UPN.
A single match is required for delete to execute.

Queries containing the # symbol must be wrapped in quotes. 
Example queries:
 - query = alice
 - query = "bob"
 - query = alice@contoso.com
 - query = 12345678-1234-1234-1234-123456789abc 
 - query = 'example_email.com#EXT#@examplesolutions.onmicrosoft.com'
