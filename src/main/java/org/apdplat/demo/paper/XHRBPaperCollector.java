package org.apdplat.demo.paper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apdplat.demo.search.Tools;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 新华日报
 * @author 杨尚川
 */
public class XHRBPaperCollector implements PaperCollector{
    private static final Logger LOG = LoggerFactory.getLogger(XHRBPaperCollector.class);
    private static final String paperName = "新华日报";
    private static String url = "http://xh.xhby.net/newxh/html/";
    private static final String paperPath = "http://xh.xhby.net/newxh/";
    private static final String hrefPrefix = paperPath+"page/1/";
    private static final String start = "node_2.htm";
    private static final String pdfCssQuery = "html body table tbody tr td table tbody tr td table tbody tr td table tbody tr td div table tbody tr td a";
    private static final SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM/dd/"); 
    
    @Override
    public List<File> collect(Date date) {
        List<String> hrefs = new ArrayList<>();
        try {
            url += sf.format(date);
            LOG.debug("url: "+url);
            String paper = url + start;
            LOG.debug("paper: "+paper);
            Document document = Jsoup.connect(paper).get();
            
            LOG.debug("pdfCssQuery: " + pdfCssQuery);
            Elements elements = document.select(pdfCssQuery);
            for(Element element : elements){
                String href = element.attr("href");
                if(href != null && href.endsWith(".pdf")){
                    LOG.debug("报纸链接："+href);
                    href = href.replace("../../../", "");
                    LOG.debug("报纸链接："+href);
                    hrefs.add(paperPath+href);
                }else{
                    LOG.debug("不是报纸链接："+href);
                }
            }            
        } catch (IOException ex) {
            LOG.error("采集出错",ex);
        }
        return downloadPaper(hrefs);
    }
    private List<File> downloadPaper(List<String> hrefs){
        final List<File> files = new ArrayList<>();
        List<Thread> ts = new ArrayList<>();
        LOG.info("报纸有"+hrefs.size()+"个版面需要下载:");
        for(final String href : hrefs){   
            Thread t = new Thread(new Runnable(){
                @Override
                public void run() {
                    File file = downloadPaper(href);
                    if(file != null){
                        files.add(file);
                    }
                }                
            });
            t.start();
            ts.add(t);
        }
        for(Thread t : ts){
            try {
                t.join();
            } catch (InterruptedException ex) {
                LOG.error("下载报纸出错：",ex);
            }
        }
        return files;
    }
    private File downloadPaper(String href){
        try{
            LOG.info("下载报纸："+href);
            LOG.debug("href："+href);
            String path = href.replace(hrefPrefix, "");
            LOG.debug("path："+path);
            String[] attrs = path.split("/");
            String pathPrefix = paperName+"/"+attrs[0];
            LOG.debug("pathPrefix："+pathPrefix);
            path = pathPrefix+"/"+attrs[1]+".pdf";
            LOG.debug("path："+path);
            File dir = new File(pathPrefix);
            if(!dir.exists()){
                dir.mkdirs();
            }
            File file = new File(path);
            Tools.copyFile(new URL(href).openStream(), file);
            LOG.info("报纸下载成功："+href);
            LOG.info("报纸保存到："+file.getAbsolutePath());
            return file;
        }catch(IOException e){
            LOG.error("报纸下载失败："+href);
        }
        return null;
    }
    @Override
    public List<File> collect() {
        return collect(new Date());
    }
    public static void main(String[] args) {
        PaperCollector paperCollector = new XHRBPaperCollector();
        List<File> files = paperCollector.collect();
        int i = 1;
        for(File file : files){
            LOG.info((i++)+" : " + file.getAbsolutePath());
        }
    }
}