import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import main.Node;
import main.Rewriter;
import main.Utils;
import org.apache.calcite.rel.RelNode;
import verify.verifyrelnode;

import java.sql.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class test_workload {
    public static void main(String[] args) throws Exception {

        //DB Config
        String path = System.getProperty("user.dir");
        JSONArray schemaJson = Utils.readJsonFile(path+"/src/main/schema.json");
        Rewriter rewriter = new Rewriter(schemaJson);

        String[] workload = Utils.readWorkloadFromFile(path+"/src/main/tpch10000.txt");
        System.out.println(workload.length);
        List addedWorkload = new ArrayList();
        JSONArray rewrittenList = new JSONArray();
        List unRewrittenList = new ArrayList();
        List failureList = new ArrayList();


        for (int i = 0; i < workload.length; i++) {
            String sql = workload[i];
            if (addedWorkload.contains(sql)) {
                continue;
            }
            System.out.println("\u001B[1;31m" + "-------------------------------------------正在改写："+ i + "\u001B[0m");
            try {
                RelNode originRelNode = rewriter.SQL2RA(sql);
                double origin_cost = rewriter.getCostRecordFromRelNode(originRelNode);
                Node resultNode = new Node(sql, originRelNode, (float) origin_cost, rewriter, (float) 0.1,null,"original query");
                Node res = resultNode.UTCSEARCH(20, resultNode,1);
                String rewritten_sql = res.state;
                if (!rewritten_sql.equalsIgnoreCase(sql)) {
                    JSONObject dataJson = new JSONObject();
                    dataJson.put("origin_cost", String.format("%.4f",origin_cost));
                    dataJson.put("origin_sql", sql);
                    dataJson.put("rewritten_cost", String.format("%.4f",rewriter.getCostRecordFromRelNode(res.state_rel)));
                    dataJson.put("rewritten_sql", res.state);
                    // equivalence check by SPES.
                    JsonObject eqres = verifyrelnode.verifyrelnode(originRelNode,res.state_rel, sql, res.state);
                    // eqres e.g.: {"decision":"true"/"false"/"unknown","plan1":"...", "plan2":"..."}
                    dataJson.put("equivalence_check",eqres);
                    rewrittenList.add(dataJson);
                }else {
                    unRewrittenList.add(sql);
                }
            } catch (Exception error) {
                error.printStackTrace();
                failureList.add(sql);
            }
            addedWorkload.add(sql);
        }
        JSONObject resultJson = new JSONObject();
        resultJson.put("rewritten", rewrittenList);
        resultJson.put("unRewritten", unRewrittenList);
        resultJson.put("failure", failureList);

        System.out.println(resultJson.toJSONString());
        Utils.writeContentStringToLocalFile(resultJson.toJSONString(), path+"/src/main/workload_rewritten_result.txt");

//        //todo query formating
//        String testSql = "SELECT * FROM (SELECT A1.*,ROWNUM RN FROM (SELECT T1.*, T4.AREA_NAME, T2.USER_NAME CONTACT_STAFF_NAME, T5.USER_NAME CLOSE_PERSON_NAME, T3.CODE_NAME EXEC_STATE_DESC, T1.CLOSE_DATE CLOSE_DATE_DESC, CASE WHEN T1.HANG_UP_SMS IS NOT NULL THEN '1' ELSE '0' END HANG_UP_SMS_STATE, T6.MKT_TIGGER_TYPE FROM view1 T1 LEFT JOIN view4 T4 ON T1.AREA_NO = T4.AREA_ID_JT LEFT JOIN view3 T2 ON T1.CONTACT_STAFF = T2.USER_ID LEFT JOIN view3 T5 ON T1.CLOSE_PERSON = T5.USER_ID LEFT JOIN view2 T3 ON T1.EXEC_STATE = T3.CODE_ID AND T1.STATUS_CODE = T3.PREV_CODE_ID AND T3.CODE_TYPE='CONTRACT_FEEDBACK' LEFT JOIN view5 T6 ON T1.MKT_CAMPAIGN_ID = T6.MKT_CAMPAIGN_ID WHERE 1=1 AND T1.CLOSE_PERSON = 12 AND T1.CLOSE_DATE >= '2021-01-18' AND T1.CLOSE_DATE <= '2022-02-18' AND T1.EXEC_STATE IN (1001,7000,1000,5000) ORDER BY T1.hang_up_sms DESC )AS A1 WHERE ROWNUM <= 30000 ) AS A2 WHERE RN > 1;";
//
//        // test3
//        testSql = "SELECT * FROM (SELECT A1.*,ROWNUM RN FROM (SELECT T1.*, T4.AREA_NAME, T2.USER_NAME CONTACT_STAFF_NAME, T5.USER_NAME CLOSE_PERSON_NAME, T3.CODE_NAME EXEC_STATE_DESC, T1.CLOSE_DATE CLOSE_DATE_DESC, CASE WHEN T1.HANG_UP_SMS IS NOT NULL THEN '1' ELSE '0' END HANG_UP_SMS_STATE, T6.MKT_TIGGER_TYPE FROM view1 T1 LEFT JOIN view4 T4 ON T1.AREA_NO = T4.AREA_ID_JT LEFT JOIN view3 T2 ON T1.CONTACT_STAFF = T2.USER_ID LEFT JOIN view3 T5 ON T1.CLOSE_PERSON = T5.USER_ID LEFT JOIN view2 T3 ON T1.EXEC_STATE = T3.CODE_ID AND T1.STATUS_CODE = T3.PREV_CODE_ID AND T3.CODE_TYPE='CONTRACT_FEEDBACK' LEFT JOIN view5 T6 ON T1.MKT_CAMPAIGN_ID = T6.MKT_CAMPAIGN_ID WHERE 1=1 AND T1.CLOSE_PERSON = 12 AND T1.CLOSE_DATE >= '2021-01-18' AND T1.CLOSE_DATE <= '2022-02-18' AND T1.EXEC_STATE IN (1001,7000,1000,5000))AS A1 WHERE ROWNUM <= 30000 ) AS A2 WHERE RN > 1 ORDER BY A2.CLOSE_DATE DESC;";
//        // test4
//        testSql = "select * from (select row_.*, aol_rownum as aol_rownum_ from (select distinct (aol.ol_id) olId, aol.ol_nbr olNbr, aol.so_date soDate, aol.rownum as aol_rownum, (select c.region_name from tab1 c where c.common_region_id = aol.order_region_id) regionName, (select c.region_name from tab1 c where c.common_region_id = aol.so_lan_id) areaName, (select cc.name from tab2 cc where cc.channel_id = aol.channel_id and rownum < 2) channelName, (select '|' || sn1.name from tab3 as sn1 where sn1.staff_id = aol.staff_id and rownum < 2) staffName, (select t.service_name from tab4 t where t.service_kind = aol.service_type) serviceName, (select so.remark from tab5 so where so.service_offer_id = aol.action_type_name) remark, aol.access_number accessNumber from tab6 aol where 1 = 1 and aol.order_region_id < 10000 and aol.so_date >= '2022-01-01 00:00:00' and aol.so_date <= '2022-01-04 00:00:00' and not exists (select 1 from ol_rule_list orl where orl.ol_id = aol.ol_id)) row_ where aol_rownum <= 40000) as table_alias where table_alias.aol_rownum_ >= 0;";
//
////    testSql = "select * from customer order by c_phone desc;";
////
////      testSql = "SELECT T1.*, T4.AREA_NAME,T2.USER_NAME CONTACT_STAFF_NAME,T5.USER_NAME CLOSE_PERSON_NAME, T3.CODE_NAME EXEC_STATE_DESC, T1.CLOSE_DATE CLOSE_DATE_DESC, CASE WHEN T1.HANG_UP_SMS IS NOT NULL THEN '1' ELSE '0' END HANG_UP_SMS_STATE, T6.MKT_TIGGER_TYPE FROM view1 T1 LEFT JOIN view4 T4 ON T1.AREA_NO = T4.AREA_ID_JT LEFT JOIN view3 T2 ON T1.CONTACT_STAFF = T2.USER_ID LEFT JOIN view3 T5 ON T1.CLOSE_PERSON = T5.USER_ID LEFT JOIN view2 T3 ON T1.EXEC_STATE = T3.CODE_ID AND T1.STATUS_CODE = T3.PREV_CODE_ID AND T3.CODE_TYPE = 'CONTRACT_FEEDBACK' LEFT JOIN view5 T6 ON T1.MKT_CAMPAIGN_ID = T6.MKT_CAMPAIGN_ID WHERE 1 = 1 AND T1.CLOSE_PERSON = 12 AND T1.CLOSE_DATE >= '2021-01-18' AND T1.CLOSE_DATE <= '2022-02-18' AND T1.EXEC_STATE IN (1001, 7000, 1000, 5000) ORDER BY T1.exec_state DESC";
////
////    testSql = "SELECT A1.*, ROWNUM RN FROM ( SELECT T1.*, T4.AREA_NAME,T2.USER_NAME CONTACT_STAFF_NAME,T5.USER_NAME CLOSE_PERSON_NAME, T3.CODE_NAME EXEC_STATE_DESC, T1.CLOSE_DATE CLOSE_DATE_DESC, CASE WHEN T1.HANG_UP_SMS IS NOT NULL THEN '1' ELSE '0' END HANG_UP_SMS_STATE, T6.MKT_TIGGER_TYPE FROM view1 T1 LEFT JOIN view4 T4 ON T1.AREA_NO = T4.AREA_ID_JT LEFT JOIN view3 T2 ON T1.CONTACT_STAFF = T2.USER_ID LEFT JOIN view3 T5 ON T1.CLOSE_PERSON = T5.USER_ID LEFT JOIN view2 T3 ON T1.EXEC_STATE = T3.CODE_ID AND T1.STATUS_CODE = T3.PREV_CODE_ID AND T3.CODE_TYPE = 'CONTRACT_FEEDBACK' LEFT JOIN view5 T6 ON T1.MKT_CAMPAIGN_ID = T6.MKT_CAMPAIGN_ID WHERE 1 = 1 AND T1.CLOSE_PERSON = 12 AND T1.CLOSE_DATE >= '2021-01-18' AND T1.CLOSE_DATE <= '2022-02-18' AND T1.EXEC_STATE IN (1001, 7000, 1000, 5000) ORDER BY T1.exec_state DESC ) AS A1 WHERE ROWNUM <= 30000";
////
//        testSql = "select * from ( select * from customer where c_custkey > 100) as c_all order by c_phone;";
//
//        testSql = "SELECT count(*) FROM job LEFT JOIN rel_hr_job RelHRJob ON job.job_id = RelHRJob.job_id LEFT JOIN rel_company_job RelCompanyJob ON job.job_id = RelCompanyJob.job_id LEFT JOIN rel_meeting_job RelMeetingJob ON job.job_id = RelMeetingJob.job_id LEFT JOIN job_addr Addr ON job.job_id = Addr.job_id WHERE job.is_delete = 0 AND RelHRJob.uid = 6860271876778758144 AND RelMeetingJob.meeting_id = 6895145840562671616";
//
//        testSql = "select distinct l_orderkey, sum(l_extendedprice + 3 + (1 - l_discount)) as revenue, o_orderkey, o_shippriority from customer, orders, lineitem where c_mktsegment = 'BUILDING' and c_custkey = o_custkey and l_orderkey = o_orderkey and o_orderdate < date '1995-03-15' and l_shipdate > date '1995-03-15' group by l_orderkey, o_orderkey, o_shippriority order by revenue desc, o_orderkey;";
//
//        testSql = "SELECT MAX(\"o_orderkey\" + 1 + 2) FROM \"orders\";";
//
//        testSql = "SELECT MAX(\"o_orderkey\" + (1 + 2)) FROM \"orders\";";
//
//        testSql = "select * from orders where o_orderpriority = 1 + 2";
//
//        testSql = "select int4ge(1,3);";
//
//        testSql = "select * from lineitem, (select * from orders where o_orderkey > 10) as v2 where v2.o_orderkey<100 and l_orderkey > 10";
//
//        testSql = "select * from orders where (o_orderpriority + o_orderkey > 10 and o_orderkey < 100+2) and (1999 + 1 < o_totalprice and o_orderpriority like 'abcd') ";
//
//        testSql = "select\n"
//                + "    n_name,\n"
//                + "    sum(l_extendedprice * (1 - l_discount)) as revenue\n"
//                + "from\n"
//                + "    customer,\n"
//                + "    orders,\n"
//                + "    lineitem,\n"
//                + "    supplier,\n"
//                + "    nation,\n"
//                + "    region\n"
//                + "where\n"
//                + "    c_custkey = o_custkey\n"
//                + "    and l_orderkey = o_orderkey\n"
//                + "    and l_suppkey = s_suppkey\n"
//                + "    and c_nationkey = s_nationkey\n"
//                + "    and s_nationkey = n_nationkey\n"
//                + "    and n_regionkey = r_regionkey\n"
//                + "    and r_name = 'MIDDLE EAST'\n"
//                + "    and o_orderdate >= date '1994-01-01'\n"
//                + "    and o_orderdate < date '1994-01-01' + interval '1' year\n"
//                + "group by\n"
//                + "    n_name\n"
//                + "order by\n"
//                + "    revenue desc;\n";
//
////    testSql = "select * from orders where 1 = 2;";
////
////    testSql = "select * from orders where 1 = 1;";
////
////    testSql = "select * from orders where o_orderpriority+o_orderkey > 'abc' and 1+1/2*0.5 > 1 and o_orderpriority like 'abc'";
//
////    testSql = "select * from orders where 1 > o_orderkey and o_orderpriority like 'abc'";
//
//        // testSql = "select distinct c1.c_custkey as ck from customer c1, customer c2, orders o where c1.c_custkey = c2.c_custkey and c1.c_custkey = o.o_orderkey";
//        testSql = "select l_returnflag, l_linestatus, sum(l_quantity) as sum_qty, sum(l_extendedprice) as sum_base_price, sum(l_extendedprice * (1 - l_discount)) as sum_disc_price, sum(l_extendedprice * (1 - l_discount) * (1 + l_tax)) as sum_charge, avg(l_quantity) as avg_qty, avg(l_extendedprice) as avg_price, avg(l_discount) as avg_disc, count(*) as count_order from lineitem where l_shipdate <= date '1998-12-01' - interval '71' days group by l_returnflag, l_linestatus order by l_returnflag, l_linestatus;";
//        testSql = testSql.replace(";", "");
//        RelNode testRelNode = rewriter.SQL2RA(testSql);
////    String output = rewriter.getRelNodeTreeJson(testRelNode).toJSONString();
////    String output = Utils.getConditionFromRelNode(testRelNode).toJSONString();
////    System.out.println("------:"+output);
//        // String sql_re = testRelNode.explain;
//        // JsonObject jsres = verify.verify(sql_re, testSql);
////    System.out.println(testSql);
//        // System.out.println(sql_re);
//        // System.out.println(jsres);
//
//        double origin_cost = rewriter.getCostRecordFromRelNode(testRelNode);
////    System.out.println("origin_cost: " + origin_cost);
//
//        Node resultNode = new Node(testSql,testRelNode, (float) origin_cost,rewriter, (float) 0.1,null,"original query");
//
//        Node res = resultNode.UTCSEARCH(5, resultNode,1);
//        System.out.println("root:"+res.state);
//        System.out.println("Original cost: "+origin_cost);
//        System.out.println("Optimized cost: "+rewriter.getCostRecordFromRelNode(res.state_rel));
//        System.out.println(Utils.generate_json(resultNode));

    }
}
