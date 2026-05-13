package com.visitor.Visitor_Backend.model;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Data
@Document(collection = "internal_meetings")
public class InternalMeeting {
    @Id
    private String id;
    private String projectName;
    private List<TeamMember> selectedEmployees;
    private String meetingDate;
    private String startTime;
	public String getProjectName() {
		return projectName;
		
		
		
	}
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	
	public String getMeetingDate() {
		return meetingDate;
	}
	public void setMeetingDate(String meetingDate) {
		this.meetingDate = meetingDate;
	}
	public String getStartTime() {
		return startTime;
	}
	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}
	public List<TeamMember> getSelectedEmployees() {
		return selectedEmployees;
	}
	public void setSelectedEmployees(List<TeamMember> selectedEmployees) {
		this.selectedEmployees = selectedEmployees;
	}
}