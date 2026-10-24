package entity;
import java.time.LocalDate;
public class Employee {
    private String employeeId, fullName, phone, email, department, status;
    private LocalDate joinDate;
    public Employee(){}
    public Employee(String id,String name,String phone,String email,String department,LocalDate joinDate,String status){
        this.employeeId=id;this.fullName=name;this.phone=phone;this.email=email;this.department=department;this.joinDate=joinDate;this.status=status;
    }
    public String getEmployeeId(){return employeeId;} public void setEmployeeId(String v){employeeId=v;}
    public String getFullName(){return fullName;} public void setFullName(String v){fullName=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getDepartment(){return department;} public void setDepartment(String v){department=v;}
    public LocalDate getJoinDate(){return joinDate;} public void setJoinDate(LocalDate v){joinDate=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
}
