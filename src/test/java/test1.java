import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jd.open.api.sdk.DefaultJdClient;
import com.jd.open.api.sdk.JdClient;
import com.jd.open.api.sdk.request.VSPzd.*;
import com.jd.open.api.sdk.response.VSPzd.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author yangwentian5
 * @Date 2022/4/2 14:27
 */
public class test1 {
    private Integer a = -2;
    public static String SERVER_URL = "https://api-dev.jd.com/routerjson";
    public static String appKey = "4F9D64CDA1E167765D5862F2E3DF8957";
    public static String appSecret = "ee4ae7a627fb43d5baa255af008d5ddf";
    public static String accessToken = "aa06a5fe295c43298b1781de0a76765fwnme";
    public static JdClient client = new DefaultJdClient(SERVER_URL, accessToken, appKey, appSecret);

    public static void main(String[] args) throws Exception {
        HashSet<Integer> hashSet = new HashSet<>();
        hashSet.add(1);
        hashSet.add(4);
        hashSet.add(3);
        hashSet.add(2);
        for (Integer i : hashSet) {
            System.out.println(i);
        }
    }

    /**
     * 公共地址新增
     */
    public static void savePublicAddress() throws Exception {
        JdClient client=new DefaultJdClient(SERVER_URL,accessToken,appKey,appSecret);
        VspAddressSavePublicAddressRequest request=new VspAddressSavePublicAddressRequest();
        request.setAddressCode("address020");
        request.setConsigneeType(2);
        request.setReceiverName("楚云飞");
        request.setOperationPin("APPtest4");
        request.setReceiverTelNo("5222222");
        request.setIsDefault(false);
        request.setAddressDetail("北京市通州区次渠嘉园东里");
        request.setReceiverEmail("1202222@qq.com");
        request.setReceiverPhoneNo("13273662222");
        VspAddressSavePublicAddressResponse response=client.execute(request);
        System.out.println(JSONObject.toJSONString(response));
    }

    /**
     * 公共地址修改
     */
    public static void updatePublicAddress() throws Exception {
        JdClient client=new DefaultJdClient(SERVER_URL,accessToken,appKey,appSecret);
        VspAddressUpdatePublicAddressRequest request=new VspAddressUpdatePublicAddressRequest();
        request.setAddressCode("address010");
        request.setReceiverName("楚云飞");
        request.setOperationPin("APPtest4");
        request.setReceiverTelNo("5222222");
        request.setIsDefault(true);
        request.setAddressDetail("北京市通州区次渠家园北里");
        request.setReceiverEmail("1202222@qq.com");
        request.setReceiverPhoneNo("13273662222");
        VspAddressUpdatePublicAddressResponse response=client.execute(request);
        System.out.println(JSONObject.toJSONString(response));
    }

    /**
     * 地址权限查询
     * TODO 为什么查询失败？
     * @throws Exception
     */
    public static void getAddressPermissions() throws Exception {
        JdClient client=new DefaultJdClient(SERVER_URL,accessToken,appKey,appSecret);
        VspAddressGetAddressPermissionsRequest request=new VspAddressGetAddressPermissionsRequest();
        request.setAddressCode("address020");
        request.setOperationPin("APPtest4");
        VspAddressGetAddressPermissionsResponse response = client.execute(request);
        System.out.println(JSONObject.toJSONString(response));
    }

    /**
     * 公共地址查询
     * @throws Exception
     */
    public static void getPublicAddress() throws Exception {
        JdClient client=new DefaultJdClient(SERVER_URL,accessToken,appKey,appSecret);
        VspAddressGetPublicAddressRequest request=new VspAddressGetPublicAddressRequest();
        request.setPageIndex(1);
        request.setOperationPin("APPtest4");
        request.setPageSize(20);
        VspAddressGetPublicAddressResponse response=client.execute(request);
        System.out.println(JSONObject.toJSONString(response.getOpenRpcResult()));
    }

    /**
     * 地址权限分配
     * @throws Exception
     */
    public static void assignPublicAddress() throws Exception {
        JdClient client=new DefaultJdClient(SERVER_URL,accessToken,appKey,appSecret);
        VspAddressAssignPublicAddressRequest request=new VspAddressAssignPublicAddressRequest();
        request.setAddressCode("address020");
        request.setUnbindPins("APPtest5");
        request.setBindPins("APPtest4");
        request.setOperationPin("APPtest4");
        request.setAllocType(3);
        VspAddressAssignPublicAddressResponse response=client.execute(request);
        System.out.println(JSONObject.toJSONString(response));
    }

    /**
     *
     * 公共地址删除
     * @throws Exception
     */
    public static void deletePublicAddress() throws Exception {
        JdClient client=new DefaultJdClient(SERVER_URL,accessToken,appKey,appSecret);
        VspAddressDeletePublicAddressRequest request=new VspAddressDeletePublicAddressRequest();
        request.setAddressCode("address010");
        request.setOperationPin("APPtest4");
        VspAddressDeletePublicAddressResponse response=client.execute(request);
        System.out.println(JSONObject.toJSONString(response));
    }
}
