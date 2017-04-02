package servlets;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import models.modules.Module;

/**
 * Servlet implementation class HomeServlet
 */
public class HomeServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public HomeServlet()
    {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
	{
		final String strCount = request.getParameter("count");
		int count = (strCount == null) ? 2 : Integer.parseInt(strCount);
		if(count < 1) { count = 1;}
		final List<Module> modules = Arrays.asList(new Module[count]);
		Collections.fill(modules, Module.WEBCAM);
		modules.set(0, Module.CONTROLS);
		//modules.set(1, Module.CONTROLS);
		request.setAttribute("modules", modules);
		request.getRequestDispatcher("/WEB-INF/pages/index.jsp").forward(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
	{
		super.doPost(request, response);
	}
}
