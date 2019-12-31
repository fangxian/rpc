package test.util;

import com.rpc.util.SerializationUtil;
import test.util.instance.Group;
import test.util.instance.User;

import java.util.Arrays;

public class SerializationUtilTest {
    public static void main(String[] args) {
        User user = User.builder().id("1").age(20).name("yfx").desc("programmer").build();
        Group group = Group.builder().id("1").name("分组1").user(user).build();
        //使用ProtostuffUtils序列化
        byte[] data = SerializationUtil.serialize(group);
        System.out.println("序列化后：" + Arrays.toString(data));
        Group result = SerializationUtil.deserialize(data, Group.class);
        System.out.println("反序列化后：" + result.toString());

    }
}
