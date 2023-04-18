package cn.tpl.opc;

import HslCommunication.Core.Types.OperateResult;
import HslCommunication.ModBus.ModbusTcpNet;

import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;


public class Main {
    static final String PLC_IP_ADDRESS = "192.0.2.19"; // PLC IP地址
    static final int PLC_PORT_NUMBER = 6000; // PLC端口号
    static final String  DATABASE_URL = "jdbc:mysql://localhost:3306/wcs_opc?useSSL=false"; // 数据库URL
    static final String DATABASE_USERNAME = "root"; // 数据库用户名
    static final String DATABASE_PASSWORD = "CHANGE_ME_DB_PASSWORD"; // 数据库密码

    public static void main(String[] args) throws IOException {


        // 连接PLC
        ModbusTcpNet plcTcpNet = new ModbusTcpNet(PLC_IP_ADDRESS,PLC_PORT_NUMBER, (byte) 1);// 创建ModbusTcpNet对象并连接到三菱PLC
        plcTcpNet.ConnectServer();// 连接到三菱PLC
        // 连接数据库

        try {
            Connection conn = DriverManager.getConnection(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
            System.out.println("数据库连接成功");
            Statement stmt = conn.createStatement();

            // 检查读码器扫码是否成功
            boolean success = checkReadCodeSuccess();

            if (!success) {
                // 向PLC写入一条数据
                OperateResult writeResult = plcTcpNet.Write("D100", 1);
                if (writeResult.IsSuccess) {
                    System.out.println("向PLC写入数据成功");
                } else {
                    System.out.println("向PLC写入数据失败：" + writeResult.Message);
                }
            } else {
                // 查询数据库是否为首次扫码
                ResultSet resultSet = stmt.executeQuery("select 1 from tablename where col = 'col' limit 1;");
                resultSet.next();
                int count = resultSet.getInt(1);

                if (count == 0) {
                    //在数据库新增一条缓冲垫数据并计数1


                    // 插入新记录到数据库
                    stmt.executeUpdate("INSERT INTO mytable(code, count) VALUES ('123456', 1)");
                } else {
                    // 获取当前使用次数
                    resultSet = stmt.executeQuery("SELECT count FROM mytable WHERE code = '123456'");
                    resultSet.next();
                    int currentCount = resultSet.getInt(1);

                    // 判断是否超过最大值
                    if (currentCount >= 10) {
                        // 向PLC写入一条数据
                        OperateResult writeResult = plcTcpNet.Write("D102", 1);
                        if (writeResult.IsSuccess) {
                            System.out.println("向PLC写入数据成功");
                        } else {
                            System.out.println("向PLC写入数据失败：" + writeResult.Message);
                        }
                    } else {
                        // 增加一次使用次数
                        OperateResult writeResult = plcTcpNet.Write("D101", 1);
                        if (writeResult.IsSuccess) {
                            System.out.println("向PLC写入数据成功");
                        } else {
                            System.out.println("向PLC写入数据失败：" + writeResult.Message);
                        }

                        // 更新数据库中的使用次数
                        stmt.executeUpdate("UPDATE mytable SET count = count + 1 WHERE code = '123456'");
                    }
                }

                conn.close();
            }

            // 断开PLC连接
            plcTcpNet.ConnectClose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static boolean checkReadCodeSuccess() throws IOException {
        // 向读码器发送指令获取扫描结果
        // 建立TCP/IP连接
        Socket socket = new Socket("192.0.2.19", 8000);
        // 获得输出流


        OutputStream outputStream = socket.getOutputStream();
        // 获得输入流
        InputStream inputStream = socket.getInputStream();
        // 创建 DataOutputStream 和 DataInputStream 对象
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        try{
            byte[] command = {0x01, 0x02, 0x03, 0x04}; // 指令内容
            dataOutputStream.write(command);
            // 从读码器接收扫描结果
            Object result = dataInputStream.read(command);
            System.out.println(result);
            boolean isSuccess = false; // 是否成功标志位
            if (result != null) {
                // 对扫描结果进行处理，判断是否成功扫描
                isSuccess = true; // 这里只是示例，实际处理方式可能不同
            }
            return isSuccess;
        }catch (Exception e){
            e.getMessage();
            return false;
        }finally {
            inputStream.close();
            outputStream.close();
            socket.close();
        }

    }

}