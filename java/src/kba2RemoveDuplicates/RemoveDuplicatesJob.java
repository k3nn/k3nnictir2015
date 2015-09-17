package kba2RemoveDuplicates;

import io.github.htools.lib.Log;
import io.github.htools.hadoop.Conf;
import io.github.htools.hadoop.io.InputFormat;
import io.github.htools.hadoop.Job;
import java.io.IOException;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;
import sentence.SentenceInputFormat;
import sentence.SentenceWritable;
import io.github.htools.hadoop.io.DayPartitioner;
import io.github.htools.hadoop.io.LongLongWritable;
import java.text.ParseException;

/**
 * Removes duplicates that have the exact same documentID in the collection,
 * within a document duplicate sentences that have the same sentence number, 
 * and strips the extracted title from non-title elements.
 * input: folder with SentenceFile per day
 * output: SentenceFile per day with duplicates removed
 * @author jeroen
 */
public class RemoveDuplicatesJob {
   public static final Log log = new Log( RemoveDuplicatesJob.class );

    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException, ParseException {
        // outdated, could estimate start and end date from input filenames
        // dates should be in YYYY-MM-DD format
        Conf conf = new Conf(args, "-i input -o output -s startdate -e enddate");
        conf.setMapMemoryMB(4096);
        conf.setMapSpeculativeExecution(false);
        DayPartitioner.setTime(conf, conf.get("startdate"), conf.get("enddate"));
        int reducers = DayPartitioner.getNumberOfReducers(conf);
        
        Job job = new Job(conf, conf.get("input"), conf.get("output"), reducers);
       
        job.setNumReduceTasks(reducers);
        job.setInputFormatClass(SentenceInputFormat.class);
        InputFormat.setNonSplitable(job);
        SentenceInputFormat.addDirs(job, conf.get("input")); 
        
        job.setMapperClass(RemoveDuplicatesMap.class);
        job.setReducerClass(RemoveDuplicatesReducer.class);
        job.setMapOutputKeyClass(LongLongWritable.class);
        job.setMapOutputValueClass(SentenceWritable.class);
        job.setPartitionerClass(DayPartitioner.class);
        job.setSortComparatorClass(LongLongWritable.SortComparator.class);

        job.setOutputFormatClass(NullOutputFormat.class);
        
        job.waitForCompletion(true);
    }
   
}
