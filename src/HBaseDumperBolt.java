

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import org.apache.hadoop.hbase.HBaseConfiguration;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.IBasicBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;
/*
 * Bolt for dumping stream data into hbase
 */
public class HBaseDumperBolt implements IBasicBolt {
    private static final long serialVersionUID = 1L;
    private static transient HBaseConnector connector = null;
    private static transient HBaseConfiguration conf = null;
    private static transient HBaseCommunicator communicator = null;
    private OutputCollector _collector;
    private Date today = null;
    private Timestamp timestamp = null;
    private ArrayList<String> colFamilyNames = new ArrayList<String>();
    private ArrayList<ArrayList<String>>  colNames = new ArrayList<ArrayList<String>>();
    private ArrayList<ArrayList<String>> colValues = new ArrayList<ArrayList<String>>();
    private ArrayList<String> colFamilyValues = new ArrayList<String>();
    private String rowKeyCheck = null, rowKey = null, fieldValue = null, tableName = null;;
    private static int counter = 0;   
    private long time;

    public HBaseDumperBolt(final String hbaseXmlLocation, final String tableName, final String rowKeyCheck, final ArrayList<String> colFamilyNames, final ArrayList<ArrayList<String>> colNames) {
        this.tableName = tableName;
        this.colFamilyNames = colFamilyNames;
        this.colNames = colNames;
        this.rowKeyCheck = rowKeyCheck;
        connector = new HBaseConnector();
        conf = connector.getHBaseConf(hbaseXmlLocation);
        communicator = new HBaseCommunicator(conf);
        //check if tableName already exists
        if (colFamilyNames.size() == colNames.size()) {
            if (!communicator.tableExists(tableName)) {
                communicator.createTable(tableName, colFamilyNames);
            }
        }
    }
    public void execute(Tuple tuple, BasicOutputCollector collector) {
        counter = 0;
        rowKey = null;
        colValues = new ArrayList<ArrayList<String>>();
        if (colFamilyNames.size() == 1) {
            for (int j = 0; j < colNames.get(0).size(); j++) {
                fieldValue = tuple.getValue(j).toString();
                if (rowKeyCheck.equals(colNames.get(0).get(j))) {
                    rowKey = fieldValue;
                }
                colFamilyValues.add(fieldValue);
            }
            colValues.add(colFamilyValues);
        } else {
            for (int i = 0; i < colFamilyNames.size(); i++) {
                for (int j = 0; j < colNames.get(i).size(); j++) {
                    fieldValue = tuple.getValue(counter).toString();
                    if (rowKeyCheck.equals(colNames.get(i).get(j))) {
                        rowKey = fieldValue;
                    }
                    colFamilyValues.add(fieldValue);
                    counter++;
                }
                colValues.add(colFamilyValues);
                colFamilyValues = new ArrayList<String>();
            }
        }
        if (rowKeyCheck.equals("timestamp") && rowKey == null) {
            today = new Date();
            timestamp = new Timestamp(today.getTime());
            time = timestamp.getTime();
            rowKey = String.valueOf(time);
        }
        communicator.addRow(rowKey, tableName, colFamilyNames, colNames, colValues);
    }
    public void prepare(Map confMap, TopologyContext context,
            OutputCollector collector) {
        _collector = collector;
    }
    public void cleanup() {
    }
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
    }
    @Override
    public Map<String, Object> getComponentConfiguration() {
        Map<String, Object> map = null;
        return map;
    }
    @Override
    public void prepare(Map stormConf, TopologyContext context) {
    }
}