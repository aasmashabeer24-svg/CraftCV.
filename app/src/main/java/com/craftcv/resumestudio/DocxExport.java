package com.craftcv.resumestudio;

import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Minimal, fully offline WordprocessingML DOCX export, no server dependency. */
public final class DocxExport {
    private DocxExport(){}

    private static String xml(String value){
        return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
            .replace("\"","&quot;").replace("'","&apos;");
    }
    private static String run(String value,boolean bold,int fontSize){
        StringBuilder result=new StringBuilder();
        String safe=xml(value);
        result.append("<w:r><w:rPr>");
        if(bold)result.append("<w:b/>");
        if(fontSize>0)result.append("<w:sz w:val=\"").append(fontSize).append("\"/>");
        result.append("</w:rPr><w:t xml:space=\"preserve\">").append(safe)
            .append("</w:t></w:r>");
        return result.toString();
    }
    private static void paragraph(StringBuilder doc,String content,boolean bold,int size,boolean spaceBefore){
        doc.append("<w:p><w:pPr>");
        if(spaceBefore)doc.append("<w:spacing w:before=\"220\" w:after=\"100\"/>");
        else doc.append("<w:spacing w:after=\"80\"/>");
        doc.append("</w:pPr>").append(run(content,bold,size)).append("</w:p>");
    }
    private static void lines(StringBuilder doc,String content){
        for(String line:content.split("\\n",-1)) paragraph(doc,line,false,20,false);
    }
    private static void zipEntry(ZipOutputStream out,String name,String content)throws Exception{
        ZipEntry entry=new ZipEntry(name);
        out.putNextEntry(entry);
        out.write(content.getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
    }
    public static byte[] create(JSONObject resume)throws Exception{
        String[] keys={"summary","experience","education","skills","projects"};
        String[] labels={"PROFESSIONAL SUMMARY","WORK EXPERIENCE","EDUCATION","SKILLS","PROJECTS"};
        StringBuilder doc=new StringBuilder();
        doc.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        doc.append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>");
        paragraph(doc,resume.optString("name","Your Name"),true,42,false);
        paragraph(doc,resume.optString("role",""),false,26,false);
        String contact=resume.optString("email","")+"   "+resume.optString("phone","")+"   "+resume.optString("location","");
        paragraph(doc,contact.trim(),false,18,false);
        for(int i=0;i<keys.length;i++){
            String content=resume.optString(keys[i],"").trim();
            if(content.isEmpty())continue;
            paragraph(doc,labels[i],true,24,true);
            lines(doc,content);
        }
        doc.append("<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/>");
        doc.append("<w:pgMar w:top=\"1000\" w:right=\"1000\" w:bottom=\"1000\" w:left=\"1000\"/></w:sectPr>");
        doc.append("</w:body></w:document>");
        String types="<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            +"<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            +"<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            +"<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
            +"<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
            +"</Types>";
        String rels="<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            +"<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            +"<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>"
            +"</Relationships>";
        ByteArrayOutputStream target=new ByteArrayOutputStream();
        try(ZipOutputStream zip=new ZipOutputStream(target)){
            zipEntry(zip,"[Content_Types].xml",types);
            zipEntry(zip,"_rels/.rels",rels);
            zipEntry(zip,"word/document.xml",doc.toString());
        }
        return target.toByteArray();
    }
}
