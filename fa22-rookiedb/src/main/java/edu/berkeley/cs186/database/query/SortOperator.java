package edu.berkeley.cs186.database.query;

import edu.berkeley.cs186.database.TransactionContext;
import edu.berkeley.cs186.database.common.Pair;
import edu.berkeley.cs186.database.common.iterator.BacktrackingIterator;
import edu.berkeley.cs186.database.query.disk.Run;
import edu.berkeley.cs186.database.table.Record;
import edu.berkeley.cs186.database.table.Schema;
import edu.berkeley.cs186.database.table.stats.TableStats;

import java.util.*;

public class SortOperator extends QueryOperator {
    protected Comparator<Record> comparator;
    private TransactionContext transaction;
    private Run sortedRecords;
    private int numBuffers;
    private int sortColumnIndex;
    private String sortColumnName;

    public SortOperator(TransactionContext transaction, QueryOperator source,
                        String columnName) {
        super(OperatorType.SORT, source);
        this.transaction = transaction;
        this.numBuffers = this.transaction.getWorkMemSize();
        this.sortColumnIndex = getSchema().findField(columnName);
        this.sortColumnName = getSchema().getFieldName(this.sortColumnIndex);
        this.comparator = new RecordComparator();
    }

    private class RecordComparator implements Comparator<Record> {
        @Override
        public int compare(Record r1, Record r2) {
            return r1.getValue(sortColumnIndex).compareTo(r2.getValue(sortColumnIndex));
        }
    }

    @Override
    public TableStats estimateStats() {
        return getSource().estimateStats();
    }

    @Override
    public Schema computeSchema() {
        return getSource().getSchema();
    }

    @Override
    public int estimateIOCost() {
        int N = getSource().estimateStats().getNumPages();
        double pass0Runs = Math.ceil(N / (double)numBuffers);
        double numPasses = 1 + Math.ceil(Math.log(pass0Runs) / Math.log(numBuffers - 1));
        return (int) (2 * N * numPasses) + getSource().estimateIOCost();
    }

    @Override
    public String str() {
        return "Sort (cost=" + estimateIOCost() + ")";
    }

    @Override
    public List<String> sortedBy() {
        return Collections.singletonList(sortColumnName);
    }

    @Override
    public boolean materialized() { return true; }

    @Override
    public BacktrackingIterator<Record> backtrackingIterator() {
        if (this.sortedRecords == null) this.sortedRecords = sort();
        return sortedRecords.iterator();
    }

    @Override
    public Iterator<Record> iterator() {
        return backtrackingIterator();
    }

    /**
     * Returns a Run containing records from the input iterator in sorted order.
     * You're free to use an in memory sort over all the records using one of
     * Java's built-in sorting methods.
     *
     * @return a single sorted run containing all the records from the input
     * iterator
     */
    public Run sortRun(Iterator<Record> records) {
        // TODO(proj3_part1): implement
        List<Record> recordList = new ArrayList<>();
        // 将迭代器转化为List容器
        while (records.hasNext()) {
            recordList.add(records.next());
        }
        // 进行排序
        recordList.sort(new RecordComparator());
        // 生成新的Run
        Run newRun = makeRun(recordList);
        return newRun;
    }

    /**
     * Given a list of sorted runs, returns a new run that is the result of
     * merging the input runs. You should use a Priority Queue (java.util.PriorityQueue)
     * to determine which record should be should be added to the output run
     * next.
     *
     * You are NOT allowed to have more than runs.size() records in your
     * priority queue at a given moment. It is recommended that your Priority
     * Queue hold Pair<Record, Integer> objects where a Pair (r, i) is the
     * Record r with the smallest value you are sorting on currently unmerged
     * from run i. `i` can be useful to locate which record to add to the queue
     * next after the smallest element is removed.
     *
     * @return a single sorted run obtained by merging the input runs
     */
    public Run mergeSortedRuns(List<Run> runs) {
        assert (runs.size() <= this.numBuffers - 1);
        // TODO(proj3_part1): implement
        PriorityQueue<Pair<Record, Integer>> queue = new PriorityQueue<>(new RecordPairComparator());
        List<Iterator<Record>> runIteratorList = new ArrayList<>();

        Run res = makeRun(); // 返回的新Run （Output Buffer）

        // 获取所有run的迭代器
        for (Run run : runs) {
            runIteratorList.add(run.iterator());
        }

        int flag = runs.size(); // 剩余还有元素的run的个数

        // 将所有run的第一个元素放入到优先队列中
        for (int i = 0; i < runIteratorList.size(); i++) {
            Iterator<Record> recordIterator = runIteratorList.get(i);
            if (recordIterator.hasNext()) {
                Record next = recordIterator.next();
                queue.add(new Pair<>(next, i));
            }else {
                flag--;
            }
        }

        // 当任一 一个run还有元素剩余时
        while (flag>0) {
            // 取出现在最小的元素
            Pair<Record, Integer> poll = queue.poll();
            Record minRecord = poll.getFirst();
            Integer runNo = poll.getSecond();
            // 如果存在下一个元素的话，向优先队列中补充来自相同run的元素
            Iterator<Record> nextRun = runIteratorList.get(runNo);
            if (nextRun.hasNext()) {
                queue.add(new Pair<>(nextRun.next(), runNo));
            }else {
                // 如果不存在下一个元素的话，说明该迭代器已经空了
                flag--;
            }
            // 将当前的最小元素写入结果run中
            res.add(minRecord);
        }


        return res;
    }

    /**
     * Compares the two (record, integer) pairs based only on the record
     * component using the default comparator. You may find this useful for
     * implementing mergeSortedRuns.
     */
    private class RecordPairComparator implements Comparator<Pair<Record, Integer>> {
        @Override
        public int compare(Pair<Record, Integer> o1, Pair<Record, Integer> o2) {
            return SortOperator.this.comparator.compare(o1.getFirst(), o2.getFirst());
        }
    }

    /**
     * Given a list of N sorted runs, returns a list of sorted runs that is the
     * result of merging (numBuffers - 1) of the input runs at a time. If N is
     * not a perfect multiple of (numBuffers - 1) the last sorted run should be
     * the result of merging less than (numBuffers - 1) runs.
     *
     * @return a list of sorted runs obtained by merging the input runs
     */
    public List<Run> mergePass(List<Run> runs) {
        // TODO(proj3_part1): implement
        int thresh = numBuffers -1; // 每次merge的数量
        int passNum = (runs.size()+thresh-1)/thresh; // 需要多少轮
        List<Run> res = new ArrayList<>(); // 结果数组
        for (int i = 0; i < passNum; i++) {
            // 每次取 thresh个runs进行merge
            List<Run> mergeRuns = runs.subList(thresh * i, Math.min(i * thresh + thresh,runs.size()));
            res.add(mergeSortedRuns(mergeRuns));
        }

        return res;
    }

    /**
     * Does an external merge sort over the records of the source operator.
     * You may find the getBlockIterator method of the QueryOperator class useful
     * here to create your initial set of sorted runs.
     *
     * @return a single run containing all of the source operator's records in
     * sorted order.
     */
    public Run sort() {
        // Iterator over the records of the relation we want to sort
        Iterator<Record> sourceIterator = getSource().iterator();

        // TODO(proj3_part1): implement
        List<Run> list = new ArrayList<>(); // 结果列表
        while (sourceIterator.hasNext()) {
            // 每次读取numBuffers页的数据
            BacktrackingIterator<Record> blockIterator = QueryOperator.getBlockIterator(sourceIterator, getSchema(), numBuffers);
            Run records = sortRun(blockIterator);
            list.add(records);
        }
        while (list.size()>1){
            list = mergePass(list);
        }
        return list.get(0); // TODO(proj3_part1): replace this!
    }

    /**
     * @return a new empty run.
     */
    public Run makeRun() {
        return new Run(this.transaction, getSchema());
    }

    /**
     * @param records
     * @return A new run containing the records in `records`
     */
    public Run makeRun(List<Record> records) {
        Run run = new Run(this.transaction, getSchema());
        run.addAll(records);
        return run;
    }
}

