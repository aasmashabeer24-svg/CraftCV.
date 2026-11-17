package com.craftcv.resumestudio;

import org.json.JSONObject;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;
import java.io.StringReader;
import static org.junit.Assert.*;

/** Real JVM checks for exported Word files; no Android emulator required. */
public class DocxExportTest {
    private Map<String,String> unzip(byte[] file) throws Exception {
        Map<String,String> entries=new HashMap<>();
        try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(file))) {
            ZipEntry entry;
            while((entry=zip.getNextEntry())!=null) {
                ByteArrayOutputStream buffer=new ByteArrayOutputStream();
                byte[] chunk=new byte[1024];int n;
                while((n=zip.read(chunk))!=-1)buffer.write(chunk,0,n);
                entries.put(entry.getName(),buffer.toString(StandardCharsets.UTF_8.name()));
                zip.closeEntry();
            }
        }
        return entries;
    }

    @Test public void docxHasRequiredOfficeOpenXmlParts() throws Exception {
        JSONObject cv=new JSONObject().put("name","Jane Doe").put("role","Software Engineer")
          .put("summary","Built accessible apps").put("skills","Java, Android");
        byte[] data=DocxExport.create(cv);
        assertNotNull(data);
        assertTrue(data.length>700);
        assertEquals('P',data[0]);
        assertEquals('K',data[1]);
        Map<String,String> entries=unzip(data);
        assertTrue(entries.containsKey("[Content_Types].xml"));
        assertTrue(entries.containsKey("_rels/.rels"));
        assertTrue(entries.containsKey("word/document.xml"));
        String doc=entries.get("word/document.xml");
        assertTrue(doc.contains("Jane Doe"));
        assertTrue(doc.contains("Software Engineer"));
        assertTrue(doc.contains("Built accessible apps"));
        assertTrue(doc.contains("SKILLS"));
        assertTrue(doc.contains("Java, Android"));
    }

    @Test public void escapedTextMakesValidWordXml() throws Exception {
        JSONObject cv=new JSONObject().put("name","A&B <CV> \"Designer\"")
          .put("role","R&D").put("summary","Use <tag> & safe XML.\nSecond line");
        String doc=unzip(DocxExport.create(cv)).get("word/document.xml");
        assertTrue(doc.contains("A&amp;B &lt;CV&gt; &quot;Designer&quot;"));
        assertTrue(doc.contains("R&amp;D"));
        assertTrue(doc.contains("Second line"));
        DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.newDocumentBuilder().parse(new InputSource(new StringReader(doc)));
    }

    @Test public void emptyOptionalFieldsStillProduceValidDocument() throws Exception {
        JSONObject cv=new JSONObject().put("name","New Resume");
        byte[] content=DocxExport.create(cv);
        Map<String,String> entries=unzip(content);
        assertTrue(entries.get("word/document.xml").contains("New Resume"));
    }
}
