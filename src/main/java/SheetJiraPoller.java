import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.FileInputStream;
import java.util.Collections;
import java.util.List;

public class SheetJiraPoller {

    static final String SPREADSHEET_ID = "1uVVVu_x8CnW6xF1dTQ8HW4h35DmWT7qtR44rGCWyDs0";
    static final String SHEET_NAME = "Sheet";
    static final int TRIGGER_COL = 4;  // 0-based, column E
    static final int TICKET_COL = 7;  // 0-based, column H
    static final int POLL_INTERVAL = 30_000;  // milliseconds

    public static void main(String[] args) throws Exception {
        Sheets sheetsService = buildSheetsService();
        System.out.println("Polling started...");

        while (true) {
            List<List<Object>> rows = fetchRows(sheetsService);

            for (int i = 1; i < rows.size(); i++) {  // skip header row
                List<Object> row = new java.util.ArrayList<>(rows.get(i));

                while (row.size() <= Math.max(TRIGGER_COL, TICKET_COL)) {
                    row.add("");
                }

                String triggerValue = row.get(TRIGGER_COL).toString();
                String ticketExists = row.get(TICKET_COL).toString();

                if (!triggerValue.isEmpty() && ticketExists.isEmpty()) {
                    System.out.println("Trigger found on row " + (i + 1));
                    // Jira ticket creation will go here later.
                    // For now, just write a placeholder so you can confirm write-back works.
                    writeTicketKey(sheetsService, i, "TEST-1");
                    System.out.println("Wrote placeholder to row " + (i + 1));
                }
            }

            Thread.sleep(POLL_INTERVAL);
        }
    }

    static Sheets buildSheetsService() throws Exception {
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream("credentials.json"))
                .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("SheetJiraPoller")
                .build();
    }

    static List<List<Object>> fetchRows(Sheets service) throws Exception {
        ValueRange response = service.spreadsheets().values()
                .get(SPREADSHEET_ID, SHEET_NAME + "!A:Z")
                .execute();
        List<List<Object>> values = response.getValues();
        return values != null ? values : Collections.emptyList();
    }

    static void writeTicketKey(Sheets service, int rowIndex, String ticketKey) throws Exception {
        char colLetter = (char) ('A' + TICKET_COL);
        String range = SHEET_NAME + "!" + colLetter + (rowIndex + 1);
        ValueRange body = new ValueRange()
                .setValues(Collections.singletonList(Collections.singletonList(ticketKey)));
        service.spreadsheets().values()
                .update(SPREADSHEET_ID, range, body)
                .setValueInputOption("RAW")
                .execute();
    }
}