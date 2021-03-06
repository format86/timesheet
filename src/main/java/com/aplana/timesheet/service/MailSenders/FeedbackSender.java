package com.aplana.timesheet.service.MailSenders;

import com.aplana.timesheet.form.FeedbackForm;
import com.aplana.timesheet.properties.TSPropertyProvider;
import com.aplana.timesheet.service.SendMailService;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.activation.DataHandler;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.util.Arrays;
import java.util.List;

public class FeedbackSender extends MailSender<FeedbackForm> {

    public FeedbackSender(SendMailService sendMailService, TSPropertyProvider propertyProvider) {
        super(sendMailService, propertyProvider);
    }

    @Override
    protected void initMessageBody(Mail mail, MimeMessage message) {
        try {
            Multipart multiPart = new MimeMultipart();

            MimeBodyPart messageText = new MimeBodyPart();
            messageText.setText(mail.getPreconstructedMessageBody(), "UTF-8", "html");
            multiPart.addBodyPart(messageText);

            for ( MultipartFile path : mail.getFilePahts()) {
                if ( ( path != null ) && ( ! path.isEmpty() ) ) {
                    MimeBodyPart attach = new MimeBodyPart();
                    attach.setDataHandler( new DataHandler(new ByteArrayDataSource(path.getBytes(), path.getContentType())) );
                    attach.setFileName( path.getOriginalFilename() );
                    multiPart.addBodyPart( attach );
                }
            }
            message.setContent(multiPart);
        } catch (Exception e) {
            logger.error("Error while init message body.", e);
        }
    }

    @Override
    protected List<Mail> getMailList(FeedbackForm params) {
        Mail mail = new Mail();

        String employeeName = sendMailService.getEmployeeFIO(params.getEmployeeId());
        String employeeEmail = sendMailService.getEmployeeEmail(params.getEmployeeId());

        mail.setCcEmail(employeeEmail);
        mail.setFromEmail(employeeEmail);
        mail.setSubject(params.getFeedbackTypeName());
        mail.setFilePahts(Arrays.asList(params.getFile1Path(), params.getFile2Path()));
        mail.setPreconstructedMessageBody(
                getMessageBody(employeeName, employeeEmail, params.getFeedbackDescription()) );
        mail.setToEmails(Arrays.asList(propertyProvider.getMailProblemsAndProposalsCoaddress()));

        return Arrays.asList(mail);
    }

    private String getMessageBody(String name, String email, String discription) {
        StringBuilder bodyTxt = new StringBuilder();

        if (StringUtils.isNotBlank(name)) {
            bodyTxt.append("Сообщение пришло от: ").append(name).append("\n");
        }
        if (StringUtils.isNotBlank(email)) {
            bodyTxt.append("С адреса: ").append(email).append("\n");
        }
        bodyTxt.append(StringEscapeUtils.escapeHtml4(discription));

        return bodyTxt.toString();
    }
}
