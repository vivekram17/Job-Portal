package com.Controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.UUID;

import com.DAO.EmployerDAO;
import com.DAO.JobSeekerDAO;
import com.Model.Employer;
import com.Model.JobSeeker;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

@WebServlet("/RegisterServlet")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2, // 2MB
    maxFileSize = 1024 * 1024 * 10,      // 10MB
    maxRequestSize = 1024 * 1024 * 50    // 50MB
)
public class RegisterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String role = req.getParameter("role");
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        if (role == null || role.isEmpty()) {
            resp.sendRedirect("register.jsp?error=roleNotSelected");
            return;
        }

        try {
            if ("seeker".equals(role)) {
                handleSeeker( req,resp, email, password);
            } else if ("employer".equals(role)) {
                handleEmployer( req,resp, email, password);
            }

            // Only redirect to login if the employer/seeker logic didn't already forward/error out
            if (!resp.isCommitted()) {
                resp.sendRedirect("login.jsp?msg=registrationSuccess");
            }
        } catch (Exception e) {
            if (!resp.isCommitted()) {
                e.printStackTrace();
                resp.sendRedirect("register.jsp?error=exceptionOccurred");
            }
        }
    }

    private void handleSeeker(HttpServletRequest req,HttpServletResponse resp, String email, String password) throws Exception {
    	JobSeekerDAO dao = new JobSeekerDAO();
    	
		if (dao.ifexists(email)) {
			// Instead of forwarding, redirect with a parameter
			resp.sendRedirect("register.jsp?error=emailExists");
			return;
		}
    	// Resume upload logic
    
        Part resumePart = req.getPart("resume");
        String originalName = Paths.get(resumePart.getSubmittedFileName()).getFileName().toString();
        // Add UUID to prevent duplicate filenames overwriting each other
        String fileName = UUID.randomUUID().toString() + "_" + originalName;

        String uploadPath = getServletContext().getRealPath("") + File.separator + "resumes";
        File dir = new File(uploadPath);
        if (!dir.exists()) dir.mkdirs();

        resumePart.write(uploadPath + File.separator + fileName);

        JobSeeker js = new JobSeeker();
        js.setName(req.getParameter("name"));
        js.setEmail(email);
        js.setPassword(password);
        
        // Handle potential number format issues
        String expStr = req.getParameter("experience");
        js.setExperienceYears(expStr != null && !expStr.isEmpty() ? Integer.parseInt(expStr) : 0);
        
        js.setLocation(req.getParameter("seekerLocation"));
        js.setResumePath("resumes/" + fileName);

        String[] skillIds = req.getParameterValues("skills");

        
        dao.registerWithSkills(js, skillIds);
    }

    private void handleEmployer( HttpServletRequest req,HttpServletResponse resp, String email, String password) throws Exception {
        EmployerDAO dao = new EmployerDAO();
        if (!dao.ifexists(email)) {
            Employer emp = new Employer();
            emp.setCompanyName(req.getParameter("companyName"));
            emp.setEmail(email);
            emp.setPassword(password);
            emp.setLocation(req.getParameter("employerLocation"));
            dao.addEmployer(emp);
        } else {
            // Instead of forwarding, redirect with a parameter
            resp.sendRedirect("register.jsp?error=emailExists");
        }
    }
}