package com.dbpj.neo4j.controller;

import com.dbpj.neo4j.VO.Neo4jGraphVO;
import com.dbpj.neo4j.VO.ResultVO;
import com.dbpj.neo4j.enums.CategoryEnum;
import com.dbpj.neo4j.enums.ResultEnum;
import com.dbpj.neo4j.node.*;
import com.dbpj.neo4j.relation.AuthorDepartmentRelation;
import com.dbpj.neo4j.relation.AuthorPaperRelation;
import com.dbpj.neo4j.service.*;
import com.dbpj.neo4j.service.impl.AuthorDepartmentRelationServiceImpl;
import com.dbpj.neo4j.service.impl.AuthorPaperRelationServiceImpl;
import com.dbpj.neo4j.service.impl.PaperConferenceRelationServiceImpl;
import com.dbpj.neo4j.service.impl.PaperFieldRelationServiceImpl;
import com.dbpj.neo4j.utils.ResultVOUtil;
import net.sf.json.JSONObject;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * @Author: Jeremy
 * @Date: 2018/12/11 14:55
 */
@RestController
@RequestMapping("/neo4j/paper")
public class PaperController {
    @Autowired
    private PaperService paperService;

    @Autowired
    private AuthorService authorService;

    @Autowired
    private ConferenceService conferenceService;

    @Autowired
    private FieldService fieldService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private AuthorDepartmentRelationServiceImpl authorDepartmentRelationService;

    @Autowired
    private AuthorPaperRelationServiceImpl authorPaperRelationService;

    @Autowired
    private PaperConferenceRelationServiceImpl paperConferenceRelationService;

    @Autowired
    private PaperFieldRelationServiceImpl paperFieldRelationService;

    @Autowired
    private SessionFactory sessionFactory;

    // 查询作者信息
    @CrossOrigin
    @PostMapping("/simple-query")
    public ResultVO getSimpleQuery(@RequestParam(value = "type") Integer type,
                                   @RequestParam(value = "conference", required = false, defaultValue = "")String conference,
                                   @RequestParam(value = "author", required = false, defaultValue = "") String author,
                                   @RequestParam(value = "field", required = false, defaultValue = "") String field,
                                   @RequestParam(value = "publishYear", required = false, defaultValue = "") Integer publishYear,
                                   @RequestParam(value = "paperTitle", required = false, defaultValue = "") String paperTitle,
                                   @RequestParam(value = "showTime", required = false, defaultValue = "10") Integer limit,
                                   @RequestParam(value = "queryTime", required = false, defaultValue = "1") Integer queryTimes){
        System.out.println("您正在做链接查询");
        System.out.println("conference: " + conference);
        System.out.println("author: " + author);
        System.out.println("field: " + field);
        System.out.println("publishYear: " + publishYear);
        System.out.println("paperTitle: " + paperTitle);


        // 如果type不为4，则忽略请求
        if(type != 2 && type != 1){
            return ResultVOUtil.error(ResultEnum.TYPE_ERROR);
        }

        if(conference.length() == 0 && author.length() == 0 && field.length() == 0 && publishYear == null && paperTitle.length() == 0){
            return ResultVOUtil.error(ResultEnum.ERROR);
        }

        String publishYearString = "";
        String r = "";
        if(paperTitle.length() == 0){
            paperTitle = ".*";
        }
        if(publishYear != null){
            publishYearString = "AND p.pYear = " + publishYear;
        }
        if(conference.length() != 0){
            conference = " MATCH r1 = (c:conference) -- (p) WHERE c.cName = \"" + conference + "\"";
            r = r + ",r1";
        }
        if(author.length() != 0){
            author = " MATCH r2 = (a:author) -- (p) WHERE a.aName = \"" + author + "\"";
            r = r + ",r2";
        }
        if(field.length() != 0){
            field = " MATCH r3 = (f:field) -- (p) WHERE f.fName = \"" + field + "\"";
            r = r + ",r3";
        }
        if(r.length() == 0) r = "p";
        else{
            r = r.substring(1);
        }

        System.out.println("MATCH (p:paper) WHERE p.pTitle =~ ('(?i).*'+\"" + paperTitle  + "\"+'.*')" + publishYearString +
                conference +
                author +
                field +
                " RETURN " + r + " LIMIT " + limit);
        // 记录执行时间
        long runtime = 0;
        long startTime = System.currentTimeMillis();   //获取开始时间

//        List<Object> Objects = paperService.findAllByAll(conference, author, field, publishYearString, paperTitle, r, limit);
        Result result = sessionFactory.openSession().query("MATCH (p:paper) WHERE p.pTitle =~ ('(?i).*'+\"" + paperTitle  + "\"+'.*')" + publishYearString +
                conference +
                author +
                field +
                " RETURN " + r + " LIMIT " + limit,new TreeMap<>());
        long endTime=System.currentTimeMillis(); //获取结束时间
        runtime += endTime-startTime;

        // 返回
        Map<String, Long> ret = new TreeMap<>();
        ret.put("time", runtime);
        Iterable resultlist = result.queryResults();
        List<TreeMap> nodes = new ArrayList<>();
        List<TreeMap> links = new ArrayList<>();
        if(r == "p"){
            for (Iterator iter = resultlist.iterator(); iter.hasNext();) {
                LinkedHashMap<String,Paper> str = (LinkedHashMap<String,Paper>)iter.next();
                TreeMap<String, Object> paper = new TreeMap<>();
                Paper p = str.get("p");
                paper.put("name", p.getPTitle());
                paper.put("value", 1);
                paper.put("category", CategoryEnum.PAPER.getCode());
                nodes.add(paper);

            }
        }
        else {
            for (Iterator iter = resultlist.iterator(); iter.hasNext(); ) {
                LinkedHashMap<String, String> str = (LinkedHashMap<String, String>) iter.next();
                System.out.println(str);
            }
        }

        Neo4jGraphVO neo4jGraphVO = new Neo4jGraphVO("force");
//        List<TreeMap> nodes = new ArrayList<>();
//        List<TreeMap> links = new ArrayList<>();
        int index = 0;
        Map<Long, Integer> indexMap = new HashMap<>();
//        for (Object Objectitem : Objects){
//            TreeMap<String, Object> node = new TreeMap<>();
//            TreeMap<String, Object> paper = new TreeMap<>();
//            TreeMap<String, Integer> link = new TreeMap<>();
//            System.out.println(Objectitem);
//
//            // 增加 author
//            Author a = authorPaperRelation.getAuthor();
//            Long aId = a.getId();
//            if (!indexMap.containsKey(aId)){
//                indexMap.put(aId, index++);
//                node.put("name", authorPaperRelation.getAuthor().getAName());
//                node.put("value", 1);
//                node.put("category", CategoryEnum.AUTHOR.getCode());
//                nodes.add(node);
//            }
//            int sourceIndex = indexMap.get(aId);
//
//            // 增加 paper
//            Paper p = authorPaperRelation.getPaper();
//            Long pId = p.getId();
//            if (!indexMap.containsKey(pId)){
//                indexMap.put(pId, index++);
//                paper.put("name", authorPaperRelation.getPaper().getPTitle());
//                paper.put("value", 1);
//                paper.put("category", CategoryEnum.PAPER.getCode());
//                nodes.add(paper);
//            }
//            int targetIndex = indexMap.get(pId);
//
//            link.put("source", sourceIndex);
//            link.put("target", targetIndex);
//
//            links.add(link);
//        }

        neo4jGraphVO.setNodes(nodes);
        neo4jGraphVO.setLinks(links);
        neo4jGraphVO.setTime(runtime);
        System.out.println(neo4jGraphVO.toString());
        return ResultVOUtil.success(neo4jGraphVO);
    }

    // 插入论文
    @CrossOrigin
    @PostMapping("/insert")
    public ResultVO insertPaperInfo(@RequestBody JSONObject jsonObject){
        String paperInfo = jsonObject.toString();
        if (paperInfo.equals("{}")) {
            return ResultVOUtil.error(ResultEnum.REQUEST_NULL);
        }
        String type = jsonObject.get("type").toString();
        if (!type.equals("2")){
            return ResultVOUtil.error(ResultEnum.TYPE_ERROR);
        }

        // 获取论文信息
        String pTitle = jsonObject.get("paperTitle").toString();
        String pYearStr = jsonObject.get("publishYear").toString();
        String pCitation = jsonObject.get("citationTime").toString();
        Integer pYear = Integer.valueOf(pYearStr);
        Paper paper = new Paper();
        paper.setPTitle(pTitle);
        paper.setPYear(pYear);
        paper.setPCitation(Integer.valueOf(pCitation));

        // 获取作者信息
        String aName = jsonObject.get("authorName").toString();
        String aUrl = jsonObject.get("authorURL").toString();
        List<Author> authorList = new ArrayList<>();
        Author author = new Author();
        author.setAName(aName);
        author.setAUrl(aUrl);
        authorList.add(author);

        // 获取会议信息
        String cName = jsonObject.get("conference").toString();

        List<Conference> conferenceList = new ArrayList<>();
        Conference conference = new Conference();
        conference.setCName(cName);
        conferenceList.add(conference);

        // 获取单位信息
        String dName = jsonObject.get("department").toString();
        List<Department> departmentList = new ArrayList<>();
        Department department = new Department();
        department.setDName(dName);
        departmentList.add(department);

        // 获取领域信息
        String fields = jsonObject.get("field").toString();
        String[] fName = fields.split(",");
        System.out.println(fName.toString());
        List<Field> fieldList = new ArrayList<>();
        for (int i=0; i<fName.length; i++){
            Field field = new Field();
            field.setFName(fName[i]);
            fieldList.add(field);
        }

        long startTime = System.currentTimeMillis();   //获取开始时间
        // 保存论文信息
        Long pId;
        List<Paper> paperRes = paperService.findByTitle(pTitle);
        if (paperRes.size() == 0){
            List<Paper> saveRes = paperService.save(paper);
            pId = saveRes.get(0).getId();
        } else{
            pId = paperRes.get(0).getId();
        }

        // 插入作者信息
        List<Long> authorIndexList = authorService.save(authorList);

        // 插入会议信息
        Long cId;
        List<Conference> conferenceRes = conferenceService.findByCName(cName);
        if (conferenceRes.size() == 0){
            List<Conference> saveRes = conferenceService.save(conference);
            cId = saveRes.get(0).getId();
        } else{
            cId = conferenceRes.get(0).getId();
        }

        // 插入单位信息
        List<Long> departmentIndexList = departmentService.save(departmentList);

        // 插入领域信息
        List<Long> fieldIndexList = fieldService.save(fieldList);

        // 插入论文-会议信息
        paperConferenceRelationService.save(pId, cId);

        // 插入论文-领域信息
        for (int i = 0; i<fieldIndexList.size(); i++){
            paperFieldRelationService.save(pId, fieldIndexList.get(i));
        }

        // 插入论文-作者信息
        for (int i=0; i<authorIndexList.size(); i++){
            authorPaperRelationService.save(authorIndexList.get(i), pId, i+1);
        }

        // 插入作者-单位信息
        authorDepartmentRelationService.save(authorIndexList, departmentIndexList, pYear);

        long endTime=System.currentTimeMillis(); //获取结束时间
        long runtime = endTime-startTime;

        Map<String, Long> ret = new TreeMap<>();
        ret.put("time", runtime);
        return ResultVOUtil.success(ret);
    }

    // 删除论文
    @CrossOrigin
    @PostMapping("/delete")
    public ResultVO deletePaperInfo(@RequestBody JSONObject jsonObject){
        String paperInfo = jsonObject.toString();
        if (paperInfo.equals("{}")) {
            return ResultVOUtil.error(ResultEnum.REQUEST_NULL);
        }
        String type = jsonObject.get("type").toString();
        if (!type.equals("2")){
            return ResultVOUtil.error(ResultEnum.TYPE_ERROR);
        }

        // 获取论文信息
        String pTitle = jsonObject.get("paperTitle").toString();
        String pYearStr = jsonObject.get("publishYear").toString();
        String pCitation = jsonObject.get("citationTime").toString();
        Integer pYear = Integer.valueOf(pYearStr);
        Paper paper = new Paper();
        paper.setPTitle(pTitle);
        paper.setPYear(pYear);
        paper.setPCitation(Integer.valueOf(pCitation));

        // 获取作者信息
        String aName = jsonObject.get("authorName").toString();
        String aUrl = jsonObject.get("authorURL").toString();
        List<Author> authorList = new ArrayList<>();
        Author author = new Author();
        author.setAName(aName);
        author.setAUrl(aUrl);
        authorList.add(author);

        // 获取会议信息
        String cName = jsonObject.get("conference").toString();

        List<Conference> conferenceList = new ArrayList<>();
        Conference conference = new Conference();
        conference.setCName(cName);
        conferenceList.add(conference);

        // 获取单位信息
        String dName = jsonObject.get("department").toString();
        List<Department> departmentList = new ArrayList<>();
        Department department = new Department();
        department.setDName(dName);
        departmentList.add(department);

        // 获取领域信息
        String fields = jsonObject.get("field").toString();
        String[] fName = fields.split(",");
        System.out.println(fName.toString());
        List<Field> fieldList = new ArrayList<>();
        for (int i=0; i<fName.length; i++){
            Field field = new Field();
            field.setFName(fName[i]);
            fieldList.add(field);
        }

        long startTime = System.currentTimeMillis();   //获取开始时间

        paperService.delete(paper);

        long endTime=System.currentTimeMillis(); //获取结束时间
        long runtime = endTime-startTime;

        Map<String, Long> ret = new TreeMap<>();
        ret.put("time", runtime);
        return ResultVOUtil.success(ret);
    }

}
