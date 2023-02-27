package com.example.chrome1.sqliteHelper.SqlLiteUtli;

import android.database.Cursor;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @ProjectName: TianDao
 * @Package: com.tiandao.Sqlite.SqlLiteUtli
 * @ClassName: SqlLiteUpdateUtil
 * @Description: 快捷更新数据库
 * @Author: 笑脸
 * @CreateDate: 2021/11/7 10:13
 * @UpdateUser: 更新者
 * @UpdateDate: 2021/11/7 10:13
 * @UpdateRemark: 更新说明
 * @Version: 1.0
 */

public class SqlLiteUpdateUtil {

    /**
     * 数据库
     *
     * @param cursor
     * @return
     */
    public static JSONArray getResults(Cursor cursor) {
        JSONArray resultSet = new JSONArray();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            int totalColumn = cursor.getColumnCount();
            JSONObject rowObject = new JSONObject();
            for (int i = 0; i < totalColumn; i++) {
                if (cursor.getColumnName(i) != null) {
                    try {
                        if (cursor.getString(i) != null) {
                            rowObject.put(cursor.getColumnName(i), cursor.getString(i));
                        } else {
                            rowObject.put(cursor.getColumnName(i), "");
                        }
                    } catch (Exception e) {
                        e.fillInStackTrace();
                    }
                }
            }
            resultSet.put(rowObject);
            cursor.moveToNext();
        }
        cursor.close();
        return resultSet;
    }


}
