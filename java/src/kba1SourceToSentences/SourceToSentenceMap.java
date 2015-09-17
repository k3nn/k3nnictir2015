package kba1SourceToSentences;

import io.github.htools.extract.HtmlTitleExtractor;
import sentence.SentenceWritable;
import io.github.htools.hadoop.io.IntLongIntWritable;
import io.github.htools.lib.Log;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Mapper;
import kbaThriftReader.ContentItem;
import kbaThriftReader.Sentence;
import kbaThriftReader.StreamItem;
import kbaThriftReader.Token;

/**
 * Reads KBA StreamItems, and writes the contents of pages from NewsDomains
 * as SentenceWritables.
 * @author jeroen
 */
public class SourceToSentenceMap extends Mapper<LongWritable, StreamItem, IntLongIntWritable, SentenceWritable> {

    public static final Log log = new Log(SourceToSentenceMap.class);
    // extracts text from within HTML tags
    HtmlTitleExtractor extractor = new HtmlTitleExtractor();
    // record used for output
    SentenceWritable outvalue = new SentenceWritable();
    NewsDomains newsDomains = NewsDomains.getInstance();
    IntLongIntWritable outkey = new IntLongIntWritable();
    int sequence = 0;

    @Override
    public void map(LongWritable key, StreamItem streamItem, Context context) throws IOException, InterruptedException {
        String url = getUrl(streamItem);
        outvalue.domain = newsDomains.getDomainForUrl(url);
        if (outvalue.domain >= 0) { // only valid domains, so other urls are ignored
            try {
                outvalue.creationtime = creationTime(streamItem);
                UUID docid = readID(streamItem);
                outvalue.documentIDLow = docid.getLeastSignificantBits();
                outvalue.documentIDHigh = docid.getMostSignificantBits();
                outvalue.setSentenceID(streamItem, 0); // preliminary document id, with just date component 
                
                // write extracted title
                String extractedTitle = extractTitle(url, streamItem);
                if (extractedTitle != null) {
                    // the extracted title is written as sentence number -1
                    // and the pre parsed title as sentence number 0
                    // in consecutive steps the pre parsed title may be replaced
                    // with the extracted title
                    outkey.set(0, outvalue.creationtime, sequence++);
                    outvalue.sentenceNumber = -1;
                    outvalue.content = extractedTitle;
                    context.write(outkey, outvalue);
                }
                
                // write all pre parsed sentences
                ArrayList<String> sentences = getSentences(streamItem);
                for (int row = 0; row < sentences.size(); row++) {
                    // the sequence ensures the records are kept in the original order
                    // which only works if input files are not split
                    outkey.set(0, outvalue.creationtime, sequence++);
                    outvalue.sentenceNumber = row;
                    outvalue.content = sentences.get(row);
                    context.write(outkey, outvalue);
                }
            } catch (ParseException ex) {
                log.exception(ex);
            }
        }
    }

    /**
     * @param streamItem
     * @return UUID of document in collection
     */
    public static UUID readID(StreamItem streamItem) {
        String name = streamItem.getDoc_id();
        if (name.length() != 32) {
            throw new IllegalArgumentException("Invalid UUID string: " + name);
        }

        long mostSigBits = Long.valueOf(name.substring(0, 8), 16);
        mostSigBits <<= 32;
        mostSigBits |= Long.valueOf(name.substring(8, 16), 16);

        long leastSigBits = Long.valueOf(name.substring(16, 24), 16);
        leastSigBits <<= 32;
        leastSigBits |= Long.valueOf(name.substring(24), 16);

        UUID uuid = new UUID(mostSigBits, leastSigBits);

        return uuid;
    }

    /**
     * @param streamItem
     * @return URL of streamItem
     */
    public String getUrl(StreamItem streamItem) {
        if (streamItem.isSetAbs_url()) {
            return new String(streamItem.getAbs_url());
        }
        return "";
    }

    /**
     * 
     * @param url
     * @param streamItem
     * @return the original cleaned title extracted from the html tags
     * in the document source
     */
    public String extractTitle(String url, StreamItem streamItem) {
        ArrayList<String> result = new ArrayList();
        ContentItem body = streamItem.getBody();
        if (body != null) {
            String b = body.getClean_html();
            if (b != null) {
                ArrayList<String> title = extractor.extract(b.getBytes());
                if (title != null && title.size() > 0) {
                    return title.get(0);
                    //return TitleFilter.filter(url, title.get(0));
                }
            }
        }
        return null;
    }
    
    /**
     * @param streamItem
     * @return list of pre parsed sentences in the streamItem
     */
    public ArrayList<String> getSentences(StreamItem streamItem) {
        ArrayList<String> result = new ArrayList();
        ContentItem body = streamItem.getBody();
        if (body != null) {
            Map<String, List<Sentence>> sentences = body.getSentences();
            for (Map.Entry<String, List<Sentence>> entry : sentences.entrySet()) {
                for (Sentence s : entry.getValue()) {
                    StringBuilder sb = new StringBuilder();
                    for (Token t : s.getTokens()) {
                        sb.append(" ").append(t.getToken());
                    }
                    if (sb.length() > 0) {
                        result.add(sb.deleteCharAt(0).toString());
                    }
                }
            }
        }
        return result;
    }

    /**
     * @param streamItem
     * @return Unix timestamp of crawled document, in seconds
     */
    public long creationTime(StreamItem streamItem) throws ParseException {
        return (long) streamItem.getStream_time().getEpoch_ticks();
    }
}
