package test.util.instance;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class User {
    private String id;
    private String name;
    private Integer age;
    private String desc;
}
