package entity;
public class Role {
    private int roleId;
    private String roleCode, roleName, description;
    public Role(){}
    public Role(int id,String code,String name,String description){roleId=id;roleCode=code;roleName=name;this.description=description;}
    public int getRoleId(){return roleId;} public void setRoleId(int v){roleId=v;}
    public String getRoleCode(){return roleCode;} public void setRoleCode(String v){roleCode=v;}
    public String getRoleName(){return roleName;} public void setRoleName(String v){roleName=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
}
