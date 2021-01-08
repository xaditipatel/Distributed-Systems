// Submitted by Aditi Patel
// ID: 1001704419

import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {
    @Override
    public String format(LogRecord record) {
        return record.getSourceClassName()+"::"
                +new Date(record.getMillis())+"::"
                +record.getMessage()+"\n";
    }
}	