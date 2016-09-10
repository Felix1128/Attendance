package com.HG.test.service.attend;

import com.HG.test.dao.attend.AttendDAO;
import com.HG.test.pojo.AttendDO;
import com.HG.test.service.attend.AttendService;

import javax.annotation.Resource;
import java.util.*;

/**
 * Created by HuaJieJie on 2016/9/5.
 */
public class AttendServiceImpl implements AttendService {

    @Resource
    private AttendDAO attendDAO;
    @Override
    public boolean SubmitAttend(String username, Date dateTime, int LeaveOrCome){

        AttendDO attendDO = new AttendDO();
        attendDO.setUsername(username);
        attendDO.setAttendTime(dateTime);
        attendDO.setType(LeaveOrCome);
        return(attendDAO.insert_attend(attendDO));

    }

    /*
     * @func : 查询某个同学在一段时间内的出勤时间，以每天为单位，如果一天有多次考勤，则计算总时长，如果一天没有出勤，则时长为0
     */
    private Long[] CalDuration(List<AttendDO> list){

        List<Long> res = new ArrayList<Long>();
        int i = 0;
        long tmp = 0;
        while (i < list.size() && list.get(i).getType() != 1) i++; //find the fist come time;
        if(i == list.size()) return null;
        Date ct = list.get(i).getAttendTime();
        int day_record =  list.get(i).getAttendTime().getDate();

        for(;i<list.size();i++)
        {

            if(list.get(i).getType() == 1)  ct = list.get(i).getAttendTime();
            else
            {
                Date lt = list.get(i).getAttendTime();
                if(lt.getDate() != day_record)
                {
                    // System.out.println(lt + "  " + lt.getDate());
                    //System.out.println(new Date(lt.getYear(),lt.getMonth(),lt.getDate(),0,0,0));
                    tmp = tmp + new Date(lt.getYear(),lt.getMonth(),lt.getDate(),0,0,0).getTime() - ct.getTime();
                    res.add(tmp);
                    tmp = lt.getTime() - new Date(lt.getYear(),lt.getMonth(),lt.getDate(),0,0,0).getTime();
                    day_record = lt.getDate();
                    //System.out.println(lt+"   "+list.get(list.size() -1).getAttendTime());
                    if(i == list.size()-1)
                        res.add(tmp);
                }
                else
                {
                    //System.out.println("ct:  "+ ct + "   lt: " +lt);
                    tmp = tmp + lt.getTime()-ct.getTime();
                    while (i < list.size() && list.get(i).getType() != 1) i++; //find the fist come time;
                    if(i == list.size())
                    {
                        res.add(tmp);
                        break;
                    }
                    ct = list.get(i).getAttendTime();
                    if(ct.getDate() != day_record )
                    {
                        //System.out.println("tmp:  "+tmp);
                        res.add(tmp);
                        day_record = ct.getDate();
                        tmp = 0;
                    }
                }
            }
        }
        return res.toArray(new Long[1]);

    }
    @Override
    public Map<Date,Long> QueryDuration(String username, Date startTime, Date endTime) {

        Map<Date,Long> result = new HashMap<Date, Long>();
        List<AttendDO> list = attendDAO.select_byUser(username, startTime, endTime);
        Long[] time = CalDuration(list);
        int i = 0;
        int day_record = list.get(0).getAttendTime().getDate();
        Date date = list.get(0).getAttendTime();
        int j = 0;
        while(i<list.size())
        {
            if(list.get(i).getAttendTime().getDate() != day_record)
            {
                result.put(date,time[j]);
                day_record = list.get(i).getAttendTime().getDate();
                date = list.get(i).getAttendTime();
                j++;
            }
            i++;
        }
        return result;
    }



    /*
     * @func : 查询某个同学在一段时间内到达的时间，以每天为单位，如果一天有多次记录，则取最早的一次
     */
    private List<AttendDO> FindSameType(List<AttendDO> list, int type)
    {
        List<AttendDO> res = new ArrayList<AttendDO>();
        for(int i = 0;i<list.size();i++)
        {
            if(list.get(i).getType() == type)
                res.add(list.get(i));
        }
        return res;
    }

    @Override
    public Date[] QueryComeTime(String username, Date startTime, Date endTime){
        List<Date> res = new ArrayList<Date>();
        List<AttendDO> list = attendDAO.select_byUser(username, startTime, endTime);
        if(list == null) return null;
        int i = 0;
        List<AttendDO> come_list = FindSameType(list, 1);
        int day_record = come_list.get(0).getAttendTime().getDate();
        res.add(come_list.get(0).getAttendTime());
        while(i<come_list.size())
        {
            if(come_list.get(i).getAttendTime().getDate()==day_record) i++;
            else{
                res.add(come_list.get(i).getAttendTime());
                day_record = come_list.get(i).getAttendTime().getDate();
                i++;
            }
        }
        return res.toArray(new Date[1]);
    }

    /*
     * @func : 查询某个同学在一段时间内离开的时间，以每天为单位，如果一天有多次记录，则取最晚的一次
     */
    @Override
    public Date[] QueryLeaveTime(String username, Date startTime, Date endTime){
        List<Date> res = new ArrayList<Date>();
        List<AttendDO> list = attendDAO.select_byUser(username, startTime, endTime);
        if(list == null) return null;
        int i = 0;
        List<AttendDO> come_list = FindSameType(list, 0);
        int day_record = come_list.get(0).getAttendTime().getDate();
        //res.add(come_list.get(0).getAttendTime());
        while(i<come_list.size())
        {
            if(come_list.get(i).getAttendTime().getDate()==day_record) i++;
            else{
                res.add(come_list.get(i-1).getAttendTime());
                day_record = come_list.get(i).getAttendTime().getDate();
                i++;
            }
        }
        res.add(come_list.get(i-1).getAttendTime());
        return res.toArray(new Date[1]);
    }


    /*
     * @func : 查询所有同学在一段时间内出勤时间，以每天为单位，如果一天有多次记录，则计算总时长，如果一天没有出勤，则时长为0
     * @return : <username, duration>
     */
    public Map<String, Long[]> QueryAllDuration(Date startTime, Date endTime){
        Map<String,Long[]> result = new HashMap<String, Long[]>();
        List<List<AttendDO>> list = attendDAO.select_ALLUser(startTime,endTime);
        for(int i = 0;i<list.size();i++)
        {
            List<AttendDO> ll = list.get(i);
            result.put(ll.get(0).getUsername(),CalDuration(ll));
        }
        return result;
    }


    /*
     * @func : 查询所有同学在某一段时间内到达的时间，如果一天有多次记录，则取最早的一次，如果一天没有出勤，则时间为null
     * @return : <username, ArriveTime>
     * @param : time的格式：YYYY-MM-DD HH-MM-SS，只取到日期即可
     */
    @Override
    public Map<String, Date[]> QueryAllComeTime(Date startTime, Date endTime){
        Map<String,Date[]> result = new HashMap<String, Date[]>();
        List<List<AttendDO>> list = attendDAO.select_ALLUser(startTime,endTime);
        for(int i = 0;i<list.size();i++)
        {
            List<Date> res = new ArrayList<Date>();
            int j = 0;
            List<AttendDO> come_list = FindSameType(list.get(i), 1);
            int day_record = come_list.get(0).getAttendTime().getDate();
            res.add(come_list.get(0).getAttendTime());
            while(j<come_list.size())
            {
                if(come_list.get(j).getAttendTime().getDate()==day_record) j++;
                else{
                    res.add(come_list.get(j).getAttendTime());
                    day_record = come_list.get(j).getAttendTime().getDate();
                    j++;
                }
            }
            result.put(come_list.get(0).getUsername(),res.toArray(new Date[1]));
        }
        return result;
    }

    /*
     * @func : 查询所有同学在某一段时间内离开的时间，如果一天有多次记录，则取最晚的一次，如果一天没有出勤，则时间为null
     * @return : <username, LeaveTime>
     * @param : time的格式：YYYY-MM-DD HH-MM-SS，只取到日期即可
     */
    @Override
    public Map<String, Date[]> QueryAllLeaveTime(Date startTime, Date endTime){
        Map<String,Date[]> result = new HashMap<String, Date[]>();
        List<List<AttendDO>> list = attendDAO.select_ALLUser(startTime,endTime);
        for(int i = 0;i<list.size();i++)
        {
            List<Date> res = new ArrayList<Date>();
            int j = 0;
            List<AttendDO> come_list = FindSameType(list.get(i), 1);
            int day_record = come_list.get(0).getAttendTime().getDate();
           // res.add(come_list.get(0).getAttendTime());
            while(j<come_list.size())
            {
                if(come_list.get(j).getAttendTime().getDate()==day_record) j++;
                else{
                    res.add(come_list.get(j-1).getAttendTime());
                    day_record = come_list.get(j).getAttendTime().getDate();
                    j++;
                }
            }
            result.put(come_list.get(0).getUsername(),res.toArray(new Date[1]));
        }
        return result;
    }
}