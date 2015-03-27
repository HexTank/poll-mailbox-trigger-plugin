package org.jenkinsci.plugins.pollmailboxtrigger.mail

import hudson.util.FormValidation
import org.jenkinsci.plugins.pollmailboxtrigger.mail.testingTools.MailboxIntegrationTest
import org.jenkinsci.plugins.pollmailboxtrigger.mail.utils.CustomProperties
import org.junit.Test

import static org.jenkinsci.plugins.pollmailboxtrigger.PollMailboxTrigger.Properties.*
import static org.jenkinsci.plugins.pollmailboxtrigger.PollMailboxTrigger.checkForEmails
import static org.jenkinsci.plugins.pollmailboxtrigger.mail.testingTools.MessageBuilder.buildMessage
import static org.junit.Assert.assertEquals

/**
 * Created by nickgrealy@gmail.com on 17/10/2014.
 */
class PollMailboxTrigger_TestConnectionTest extends MailboxIntegrationTest {


    public static final int SEED_EMAILS = 3

    @Test
    def void testConnectionWithMissingConfigShouldBeError() {
        // run
        def validation = checkForEmails(new CustomProperties(), logger, true, trigger)
        // assert
        assertEquals FormValidation.Kind.ERROR, validation.kind
        assertEquals 'hudson.util.FormValidation$1: Error : Email property &#039;host&#039; is required!, Email property &#039;storeName&#039; is required!, Email property &#039;username&#039; is required!, Email property &#039;password&#039; is required!', validation.toString()
    }

    @Test
    def void testConnectionWithNoFolderShouldBeError() {
        // run
        def validation = checkForEmails(config, logger, true, trigger)
        // assert
        assertEquals FormValidation.Kind.ERROR, validation.kind
        assertEquals 'hudson.util.FormValidation$1: Connected to mailbox. <br>Please set the &#039;folder=XXX&#039; parameter to one of the following values: <br>Folders: ', validation.toString()
    }

    @Test
    def void testConnectionWithNoEmailsAndNoFiltersIsOk() {
        // run
        def validation = checkForEmails(config.put(folder, 'INBOX'), logger, true, trigger)
        // assert
        assertEquals FormValidation.Kind.OK, validation.kind
        assertEquals 'hudson.util.FormValidation$1: Connected to mailbox. <br>Searching folder...<br>Found matching email(s) : 0. <br>Result: Success!', validation.toString()
    }

    @Test
    def void testConnectionWithOneEmailAndNoFiltersIsOk() {
        // setup
        inmemoryMailbox.add(buildMessage('foobar'))
        // run
        def validation = checkForEmails(config.put(folder, 'INBOX'), logger, true, trigger)
        // assert
        assertEquals FormValidation.Kind.OK, validation.kind
        assertEquals 'hudson.util.FormValidation$1: Connected to mailbox. <br>Searching folder...<br>Found matching email(s) : 1. <br>Result: Success!', validation.toString()
    }

    @Test
    def void testConnectionWithManyEmailsAndNoFiltersIsOk() {
        // setup
        int numEmails = SEED_EMAILS
        (1..numEmails).each { inmemoryMailbox.add(buildMessage("foobar$it")) }
        // run
        def validation = checkForEmails(config.put(folder, 'INBOX'), logger, true, trigger)
        // assert
        assertEquals FormValidation.Kind.OK, validation.kind
        assertEquals "hudson.util.FormValidation\$1: Connected to mailbox. <br>Searching folder...<br>Found matching email(s) : $numEmails. <br>Result: Success!".toString(), validation.toString()
    }

    @Test
    def void testConnectionWithManyEmailsAndSubjectFilterIsOk() {
        // setup
        int numEmails = SEED_EMAILS
        (1..numEmails).each { inmemoryMailbox.add(buildMessage("foobar$it")) }  // these should be ignored
        inmemoryMailbox.add(buildMessage("Jenkins > Foobar!"))                  // this should be picked up (by subject)
        // run
        def validation = checkForEmails(config.put(folder, 'INBOX').put(subjectContains, 'Jenkins >'), logger, true, trigger)
        // assert
        assertEquals FormValidation.Kind.OK, validation.kind
        assertEquals 'hudson.util.FormValidation$1: Connected to mailbox. <br>Searching folder...<br>Found matching email(s) : 1. <br>Result: Success!', validation.toString()
    }

    @Test
    def void testConnectionWithManyEmailsAndDateFilterIsOk() {
        // setup
        int numEmails = SEED_EMAILS
        (1..numEmails).each { inmemoryMailbox.add(buildMessage("foobar$it")) }  // these should be ignored
        inmemoryMailbox.add(buildMessage("foobar", new Date()))               // this should be picked up (by date)
        // run
        def validation = checkForEmails(config.put(folder, 'INBOX').put(receivedXMinutesAgo, '1'), logger, true, trigger)
        // assert
        assertEquals FormValidation.Kind.OK, validation.kind
        assertEquals 'hudson.util.FormValidation$1: Connected to mailbox. <br>Searching folder...<br>Found matching email(s) : 1. <br>Result: Success!', validation.toString()
    }

    @Test
    def void testConnectionWithManyEmailsAndAllFiltersIsOk() {
        // setup
        int numEmails = SEED_EMAILS
        (1..numEmails).each { inmemoryMailbox.add(buildMessage("foobar$it")) }  // these should be ignored
        inmemoryMailbox.add(buildMessage("foobar", new Date()))                 // this should NOT be picked up
        inmemoryMailbox.add(buildMessage("Jenkins > Foobar!"))                  // this should NOT be picked up
        inmemoryMailbox.add(buildMessage("Jenkins > Foobar!", new Date()))      // this should be picked up (by subject and date)
        // run
        def validation = checkForEmails(config.put(folder, 'INBOX').put(subjectContains, 'Jenkins >').put(receivedXMinutesAgo, '1'), logger, true, trigger)
        // assert
        assertEquals FormValidation.Kind.OK, validation.kind
        assertEquals 'hudson.util.FormValidation$1: Connected to mailbox. <br>Searching folder...<br>Found matching email(s) : 1. <br>Result: Success!', validation.toString()
    }

}