package com.gw.utils;

import java.util.Date;

import javax.mail.internet.MimeMessage;

import com.google.api.services.gmail.Gmail;
import com.gw.jpa.GWUser;
import com.gw.tools.UserTool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
public class EmailService {

	@Autowired
	UserTool ut;

	public void send_resetpassword(GWUser user, String site_url){

		EmailMessage message = new EmailMessage();

		message.setSubject("You Requested to Reset Your Password on Geoweaver");

		message.setTo_address(user.getEmail());

		String token = new RandomString(30).nextString();

		String reset_url = site_url + "reset_password?token=" + token;

		ut.token2userid.put(token, user.getId());

		ut.token2date.put(token, new Date());

		message.setBody("Hello zsun@gmu.edu! \n"+

		" Someone has requested a link to change your password. You can do this through the link below.\n"+
		
		"  <a href=\""+reset_url+"\">Change my password</a> \n" +
		
		" If you didn't request this, please ignore this email. \n"+
		
		"Your password won't change until you access the link above and create a new one. \n");

		this.sendmail(message);

	}
	
	public void sendmail(EmailMessage emailmessage)   {

		try {
			
			Gmail service = GmailAPI.getGmailService();

			GmailOperations gmailOperations = new GmailOperations();

			MimeMessage Mimemessage = gmailOperations.createEmail(emailmessage.getTo_address(),
					"geoweaver.app@gmail.com",
					emailmessage.getSubject(), 
					emailmessage.getBody());

			com.google.api.services.gmail.model.Message msg = gmailOperations.createMessageWithEmail(Mimemessage);

			service.users().messages().send("Geoweaver Service Team", msg).execute();
			
		}catch(Exception e) {
			
			e.printStackTrace();
			
			System.err.print("Failed to send email..");
			
		}
		
		
	}
	
}