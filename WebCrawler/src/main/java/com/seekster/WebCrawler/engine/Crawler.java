package com.seekster.WebCrawler.engine;

import com.seekster.WebCrawler.rabbitmq.MessageSender;
import com.seekster.WebCrawler.rabbitmq.message.ContentMessage;
import com.seekster.WebCrawler.registry.ApplicationContextProvider;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

import static com.seekster.WebCrawler.rabbitmq.RabbitMqConstants.QUEUE_CRAWLER_CONTENT_SEND;

public class Crawler extends WebCrawler {

    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|gif|jpg"
            + "|png|mp3|mp4|zip|gz))$");
    private final MessageSender messageSender = ApplicationContextProvider.getApplicationContext().getBean(MessageSender.class);

    /**
     * This method receives two parameters. The first parameter is the page
     * in which we have discovered this new url and the second parameter is
     * the new url. You should implement this function to specify whether
     * the given url should be crawled or not (based on your crawling logic).
     * In this example, we are instructing the crawler to ignore urls that
     * have css, js, git, ... extensions and to only accept urls that start
     * with "https://www.ics.uci.edu/". In this case, we didn't need the
     * referringPage parameter to make the decision.
     */
    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href = url.getURL().toLowerCase();
        return !FILTERS.matcher(href).matches()
                && href.startsWith(url.getParentUrl());
    }

    /**
     * This function is called when a page is fetched and ready
     * to be processed by your program.
     */
    @Override
    public void visit(Page page) {
        String url = page.getWebURL().getURL();
        System.out.println("URL: " + url);
        if (page.getParseData() instanceof HtmlParseData htmlParseData && !page.isRedirect()) {
            ContentMessage message = new ContentMessage();
            message.setUrl(url);
            message.setTitle(htmlParseData.getTitle());
            String content = Jsoup.parse(htmlParseData.getHtml()).wholeText();
            String cleanContent = content.replaceAll("\\s+", " ").trim();
            message.setContent(cleanContent);
            messageSender.send(message, QUEUE_CRAWLER_CONTENT_SEND);
        }
    }
}
