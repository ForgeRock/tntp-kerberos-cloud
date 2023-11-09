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
1. Replace the AuthenticateLocalUser Groovy script in the [IdentityAssertionRoute.json](https://github.com/ForgeRock/gateway-communication-node/blob/main/sample/IdentityAssertionRoute.json) with the sample [Kerberos Groovy script](https://github.com/ForgeRock/tntp-kerberos-cloud/blob/main/samples/ValidateKerberosTicket.groovy)
2. Ensure the service account credentials are properly configured in the Kerberos Groovy script.
3. Ensure the spengo.conf file location is properly configured in the Kerberos Groovy script.
4. Modify the CreateAssertionJwt to include the **attribute.kerbUsername**. You can simply replace line 103 in the [IdentityAssertionRoute.json](https://github.com/ForgeRock/gateway-communication-node/blob/main/sample/IdentityAssertionRoute.json#L103) with
      >"samaccountname": "${attribute.kerbUsername}"
### Identity Cloud Configuration
1. In the [sample Journey](https://github.com/ForgeRock/gateway-communication-node/blob/main/sample/IG-ForShow-journeyExport-alpha-realm-openam-tntp-ig-testing.forgeblocks.com-2023-10-16T18_46_17.252Z.json) ensure the **samaccountname** is mapped to the **username**.
2. Ensure a test user exists in your Identity Cloud environment with the same username as the samaccountname of the user you plan on testing within.
### Test
Run the sample Journey from the Client browser with a valid Kerberos ticket that can reach both Identity Cloud and Identity Gateway.  The result should be the user can login with a Kerberos ticket in Identity Cloud.
