package lucene;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

public class LuceneExample {

    public void indexMessages(Directory directory, Message[] messages) {
        IndexWriter writer = null;
        try {
            Analyzer analyzer = new StandardAnalyzer();
            writer = new IndexWriter(directory, analyzer, MaxFieldLength.UNLIMITED);
            for (Message message : messages) {
                Document document = new Document();
                if (message != null) {
                    if (message.getSubject() == null) {
                        System.out.println("Skipped...");
                    }
                    else {
                        document.add(new Field("fromAddress", ((InternetAddress) message.getFrom()[0]).getAddress(), Field.Store.YES, Field.Index.NOT_ANALYZED));
                        document.add(new Field("fromPersonal", ((InternetAddress) message.getFrom()[0]).getPersonal(), Field.Store.YES, Field.Index.NOT_ANALYZED));
                        document.add(new Field("sentDate", DateTools.dateToString(message.getSentDate(), DateTools.Resolution.MINUTE), Field.Store.YES, Field.Index.NOT_ANALYZED));
                        document.add(new Field("subject", message.getSubject(), Field.Store.YES, Field.Index.ANALYZED));
                        document.add(new Field("size", Integer.toString(message.getSize()), Field.Store.YES, Field.Index.NO));
                        if (message.getContent() instanceof String) {
                            document.add(new Field("content", (String) message.getContent(), Field.Store.NO, Field.Index.ANALYZED));
                        }
                        writer.addDocument(document);
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error indexing message.", e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ex) {
                    // Can not close
                }
            }
        }
    }

    public void searchMessages(Directory dir, String query) {
        try {
            Query q = new QueryParser("content", new StandardAnalyzer()).parse(query);
            IndexSearcher s = new IndexSearcher(dir);
            TopFieldDocs docs = s.search(q, null, 100, new Sort("sentDate", true));
            for (ScoreDoc scoreDoc : docs.scoreDocs) {
                Document doc = s.doc(scoreDoc.doc);
                System.out.println("Subject: " + doc.get("subject"));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error by query.", e);
        }
    }

    public Message[] getMessages(File file) {
        try {
            Properties p = new Properties();
            Session session = Session.getInstance(p);
            String url = "mstor:" + file.getAbsolutePath();
            Store store = session.getStore(new URLName(url));
            store.connect("", "");
            Folder folder = store.getDefaultFolder();
            folder.open(Folder.READ_ONLY);
            System.out.println("Messages in file " + file.getAbsolutePath() + " found: " + folder.getMessageCount());
            Message[] messages = folder.getMessages(1, folder.getMessageCount());
            folder.close(false);
            store.close();
            return messages;
        } catch (Exception e) {
            throw new IllegalStateException("Error loading mails from mailbox.", e);
        }
    }


    public static void main(String[] args) {
        Directory directory = new RAMDirectory();
        LuceneExample example = new LuceneExample();
        Message[] messages = example.getMessages(new File("src\\main\\resources\\2008-November.txt"));
        example.indexMessages(directory, messages);
        example.searchMessages(directory, "SMTP");
    }


}
