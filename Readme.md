<!--
*
 * This code is to be used exclusively in connection with ForgeRock’s software or services. 
 * ForgeRock only offers ForgeRock software or services to legal entities who have entered 
 * into a binding license agreement with ForgeRock.  
 *
-->
## Kerberos Authentication in Identity Cloud Using Identity Gateway
### Prerequisites
1. Kerberos authentication must be enabled in Active Directory.
2. Install Identity Gateway and ensure the Java environment truststore is setup to trust server certificates.
3. Ensure the Identity Gateway and the Active Directory servers can reach one another.
4. Synchronize the time between the Identity Gateway and the Active Directory server.
5. Create krb5.conf in the Java environment /lib/security/ directory.  Here is a sample [krb5.conf](https://github.com/ForgeRock/tntp-kerberos-cloud/blob/main/samples/krb5.conf) file.
6. Create spengo.conf.  Here is a sample [spengo.conf](https://github.com/ForgeRock/tntp-kerberos-cloud/blob/main/samples/spnego.conf) file. You will need the path of this file in a later step.
7. Create a service account for Identity Gateway.  
8. Issue the following command on the Active Directory server assuming **demoig.server.frdpcloud.org** is the fully qualified domain name of the Identity Gateway Server and **igsa** is the service account samaccountname: 
      >setspn -s HTTP/demoig.server.frdpcloud.org igsa
9. Follow the [Gateway Communication node](https://backstage.forgerock.com/docs/idcloud/latest/release-notes/rapid-channel/auth-node-gateway-comm.html) setup.
10. Ensure the Client you will use to test the Kerberos Authentication has a valid Kerberos Ticket.
11. Ensure the Client you will use to test the Kerberos Authentication has added the Identity Gateway FQDN to the sites in the following zone -
      >IE -> Internet Options -> Security -> Local Intranet -> Sites -> Advance
### Identity Gateway Route Configuration
1. Replace the AuthenticateLocalUser Groovy script in the [sample route](https://github.com/ForgeRock/gateway-communication-node/blob/main/sample/IdentityAssertionRoute.json) with the sample [Kerberos Groovy script](https://github.com/ForgeRock/tntp-kerberos-cloud/blob/main/samples/ValidateKerberosTicket.groovy)
2. Ensure the service account credentials are properly configured in the Kerberos Groovy script.
3. Ensure the spengo.conf file location is properly configured in the Kerberos Groovy script.  
