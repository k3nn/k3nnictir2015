package stream4ClusterSentencesPurge.IDF;

import static io.github.k3nn.ClusteringGraph.getUnstemmedTokenizer;
import io.github.htools.lib.Log;
import java.io.IOException;
import kba1SourceToSentences.NewsDomains;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Mapper;
import sentence.SentenceWritable;
import io.github.htools.collection.HashMap3;
import io.github.htools.extract.DefaultTokenizer;
import io.github.htools.hadoop.io.LongBoolWritable;
import io.github.htools.type.KV;
import kba1SourceToSentences.TitleFilter;
import static stream4ClusterSentences.ClusterSentencesJob.getRelevantDocs;

/**
 *
 * @author jeroen
 */
public class ClusterSentencesMap extends Mapper<LongWritable, SentenceWritable, LongBoolWritable, SentenceWritable> {

    public static final Log log = new Log(ClusterSentencesMap.class);
    Configuration conf;
    static DefaultTokenizer tokenizer = getUnstemmedTokenizer();
    NewsDomains domain = NewsDomains.getInstance();
    HashMap3<String, Long, Boolean> relevantdocs;
    LongBoolWritable outkey = new LongBoolWritable();

    enum Counter {

        candidate,
        noncandidate
    }

    @Override
    public void setup(Context context) throws IOException {
        conf = context.getConfiguration();
        relevantdocs = getRelevantDocs(conf);
    }

    @Override
    public void map(LongWritable key, SentenceWritable value, Context context) throws IOException, InterruptedException {
        KV<Long, Boolean> docparams = relevantdocs.get(value.getDocumentID());
        if (docparams != null) {
            if (value.sentenceNumber != 0) { // row 0 is duplicate for extracted title -1
                if (value.sentenceNumber == -1) {
                    value.sentenceNumber = 0;
                    String dom = domain.getHostPart(value.domain);
                    value.content = TitleFilter.filterHost(dom, value.content);
                }
                    outkey.set(value.sentenceID, docparams.value);
                    context.write(outkey, value);
                    if (docparams.value) {
                        context.getCounter(Counter.candidate).increment(1);
                    } else {
                        context.getCounter(Counter.noncandidate).increment(1);
                    }
            }
        }
    }
}
