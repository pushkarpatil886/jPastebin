package org.jpaste.pastebin;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jpaste.exceptions.PasteException;
import org.jpaste.pastebin.exceptions.ParseException;
import org.jpaste.utils.web.Post;
import org.jpaste.utils.web.Web;
import org.jpaste.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.fasterxml.jackson.databind.ObjectMapper;


public class Pastebin {
    
    public static final String API_POST_LINK = "https://pastebin.com/api/api_post.php";
   
    public static final String API_LOGIN_LINK = "https://pastebin.com/api/api_login.php";

    
    public static final String API_SCRAPING_LINK = "https://pastebin.com/api_scraping.php";

    
    public static String getContents(String pasteKey) {
        return PastebinLink.getContents(pasteKey);
    }

   
    public static URL pastePaste(String developerKey, String contents) throws PasteException {
        return pastePaste(developerKey, contents, null);
    }

   
    public static URL pastePaste(String developerKey, String contents, String title) throws PasteException {
        return newPaste(developerKey, contents, title).paste().getLink();
    }

    
    public static PastebinPaste newPaste(String developerKey, String contents, String title) {
        PastebinPaste paste = new PastebinPaste(developerKey, contents);
        paste.setPasteTitle(title);
        return paste;
    }

    
    public static PastebinPaste newPaste(String developerKey, String contents) {
        return newPaste(developerKey, contents, null);
    }

    
    public static PastebinLink[] getTrending(String developerKey) throws ParseException {
        if (developerKey == null || developerKey.isEmpty()) {
            throw new IllegalArgumentException("Developer key can't be null or empty.");
        }
        Post post = new Post();
        post.put("api_dev_key", developerKey);
        post.put("api_option", "trends");

        String response = Web.getContents(API_POST_LINK, post);

        if (response.startsWith("<paste>")) {
            // success
            try {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                response = "<dummy>" + response + "</dummy>"; // requires root
                                                              // element
                Document doc = dBuilder.parse(new InputSource(new ByteArrayInputStream(response.getBytes("utf-8"))));
                doc.getDocumentElement().normalize();

                NodeList nodes = doc.getElementsByTagName("paste");
                ArrayList<PastebinLink> pastes = new ArrayList<PastebinLink>(nodes.getLength());
                for (int i = 0; i < nodes.getLength(); i++) {
                    Node node = nodes.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;

                        String pasteFormat = XMLUtils.getText(element, "paste_format_short");
                        String title = XMLUtils.getText(element, "paste_title");
                        int visibility = Integer.parseInt(XMLUtils.getText(element, "paste_private"));
                        int hits = Integer.parseInt(XMLUtils.getText(element, "paste_hits"));

                        long expireDate = Long.parseLong(XMLUtils.getText(element, "paste_expire_date"));
                        long pasteDate = Long.parseLong(XMLUtils.getText(element, "paste_date"));

                        URL pasteURL = new URL(XMLUtils.getText(element, "paste_url"));

                        PastebinPaste paste = new PastebinPaste();
                        paste.setPasteFormat(pasteFormat);
                        paste.setPasteTitle(title);
                        paste.setVisibility(visibility);
                        paste.setPasteExpireDate(expireDate == 0L ? PasteExpireDate.NEVER
                                : PasteExpireDate.getExpireDate((int) (expireDate - pasteDate)));

                        PastebinLink pastebinLink = new PastebinLink(paste, pasteURL, new Date(pasteDate * 1000));
                        pastebinLink.setHits(hits);

                        pastes.add(pastebinLink);
                    }
                }

                return pastes.toArray(new PastebinLink[pastes.size()]);
            } catch (Exception e) {
                throw new ParseException("Failed to parse pastes: " + e.getMessage());
            }

        }

        throw new ParseException("Failed to parse pastes: " + response);
    }

   
    public static PastebinLink[] getMostRecent(Post post) throws ParseException {
        String url = API_SCRAPING_LINK;
        if (post != null && !post.getPost().isEmpty()) {
            url += "?" + post.getPost();
        }

        String response = Web.getContents(url);

        if (response == null || response.isEmpty()
                || !(response.charAt(0) == '[' && response.charAt(response.length() - 2) == ']')) {
            throw new ParseException("Failed to parse pastes: " + response);
        }

        ArrayList<Object> listData = getJSonData(response);

        ArrayList<PastebinLink> listPastebinLink = new ArrayList<>(listData.size());
        for (Object object : listData) {
            Map<String, Object> tempMap = (Map<String, Object>) object;
            PastebinPaste pastebinPaste = new PastebinPaste();
            pastebinPaste.setPasteFormat(tempMap.get("syntax").toString());
            String pasteTitle = tempMap.get("title").toString();
            pastebinPaste.setPasteTitle(pasteTitle == null ? "" : pasteTitle);
            long pasteExpireDate = Long.parseLong(tempMap.get("expire").toString());
            long pasteDate = Long.parseLong(tempMap.get("date").toString());
            pastebinPaste.setPasteExpireDate(pasteExpireDate == 0L ? PasteExpireDate.NEVER
                    : PasteExpireDate.getExpireDate((int) (pasteExpireDate - pasteDate)));
            pastebinPaste.setVisibility(PastebinPaste.VISIBILITY_PUBLIC);
            // All the pastes retrieved from this api are public.

            PastebinLink pastebinLink = null;
            try {
                pastebinLink = new PastebinLink(pastebinPaste, new URL(tempMap.get("full_url").toString()),
                        new Date(pasteDate * 1000));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            if (pastebinLink != null) {
                listPastebinLink.add(pastebinLink);
            }
        }

        return listPastebinLink.toArray(new PastebinLink[listPastebinLink.size()]);
    }

    private static ArrayList<Object> getJSonData(String response) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            ArrayList<Object> data = mapper.readValue(response, ArrayList.class);
            return data;
        } catch (IOException e) {

            e.printStackTrace();
        }
        return null;
    }

}
