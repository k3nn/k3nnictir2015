package stream3DocStream;

import io.github.htools.io.buffer.BufferDelayedWriter;
import io.github.htools.io.buffer.BufferReaderWriter;
import io.github.htools.hadoop.tsv.Writable;
/**
 *
 * @author jeroen
 */
public class DocumentStreamWritable extends Writable<DocumentStreamFile> {
    public String docid;
    public long creationtime;
    public boolean isCandidate;
    
    @Override
    public void read(DocumentStreamFile f) {
        this.docid = f.docid.get();
        this.creationtime = f.creationtime.get();
        this.isCandidate = f.isCandidate.get();
    }

    @Override
    public void write(BufferDelayedWriter writer)  {
        writer.write(docid);
        writer.write(creationtime);
        writer.write(isCandidate);
    }

    @Override
    public void readFields(BufferReaderWriter reader) {
        docid = reader.readString();
        creationtime = reader.readLong();
        isCandidate = reader.readBoolean();
    }

    @Override
    public void write(DocumentStreamFile file) {
        file.docid.set(docid);
        file.creationtime.set(creationtime);
        file.isCandidate.set(isCandidate);
        file.write();
    }
}
