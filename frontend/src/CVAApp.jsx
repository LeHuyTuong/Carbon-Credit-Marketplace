import { useState } from "react";
import { Routes, Route } from "react-router-dom";
import TopBar from "./pages/Dashboard/CVA/global/Topbar";
import Sidebar from "./pages/Dashboard/CVA/global/Sidebar";
import Dashboard from "./pages/Dashboard/CVA/dashboard";
import Report_Management from "./pages/Dashboard/CVA/Report/index.jsx";
import Credit_Management from "./pages/Dashboard/CVA/Credit/index.jsx";
import Company_Management from "./pages/Dashboard/CVA/Company/index.jsx";
import Log_Management from "./pages/Dashboard/CVA/Log/index.jsx";
import Project_Management from "./pages/Dashboard/CVA/Project/index.jsx";
import View_Profile from "./pages/Dashboard/CVA/profile/view.jsx";
import Edit_Profile from "./pages/Dashboard/CVA/profile/edit.jsx";
import Report_View from "./pages/Dashboard/CVA/Report/view.jsx";
import Credit_View from "./pages/Dashboard/CVA/Credit/view.jsx";
import Company_View from "./pages/Dashboard/CVA/Company/view.jsx";
import Log_View from "./pages/Dashboard/CVA/Log/view.jsx";
import Project_View from "./pages/Dashboard/CVA/Project/view.jsx";
import Registration_Project_Management from "./pages/Dashboard/CVA/Registration_Project/index.jsx";
import Registration_Project_View from "./pages/Dashboard/CVA/Registration_Project/view.jsx";
import Registraion_Project_Edit from "./pages/Dashboard/CVA/Registration_Project/edit.jsx";
import Bar from "./pages/Dashboard/CVA/bar";
import Form from "./pages/Dashboard/CVA/form";
import Line from "./pages/Dashboard/CVA/line";
import Pie from "./pages/Dashboard/CVA/pie";
import FAQ from "./pages/Dashboard/CVA/faq";
import Geography from "./pages/Dashboard/CVA/geography";
import { CssBaseline, ThemeProvider } from "@mui/material";
import { ColorModeContext, useMode } from "./themeCVA";
import Calendar from "./pages/Dashboard/CVA/calendar/Calendar.jsx";
//  import css admin riêng
import "./admin.css";


function App() {
  const [theme, colorMode] = useMode();
  const [isSidebar, setIsSidebar] = useState(true);

  return (
    <ColorModeContext.Provider value={colorMode}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <div className="app">
          <Sidebar isSidebar={isSidebar} />
          <main className="content">
            <TopBar setIsSidebar={setIsSidebar} />
            <Routes>
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/report_management" element={<Report_Management />} />
              <Route path="/credit_management" element={<Credit_Management />} />
              <Route path="/company_management" element={<Company_Management />} />
              <Route path="/log_management" element={<Log_Management />} />
              <Route path="/project_management" element={<Project_Management />} />
              <Route path="/form" element={<Form />} />
              <Route path="/bar" element={<Bar />} />
              <Route path="/pie" element={<Pie />} />
              <Route path="/line" element={<Line />} />
              <Route path="/faq" element={<FAQ />} />
              <Route path="/calendar" element={<Calendar />} />
              <Route path="/geography" element={<Geography />} />
              <Route path="/view_profile_cva" element={<View_Profile />} />
              <Route path="/edit_profile_cva" element={<Edit_Profile />} />
              <Route path="/view_company/:id" element={<Company_View />} />
              <Route path="/view_credit/:id" element={<Credit_View />} />
              <Route path="/view_report/:id" element={<Report_View />} />
              <Route path="/view_log/:id" element={<Log_View />} />
              <Route path="/view_project/:id" element={<Project_View />} />
              <Route path="/registration_project_management" element={<Registration_Project_Management />} />
              <Route path="/view_registration_project/:id" element={<Registration_Project_View />} />
              <Route path="/edit_registration_project/:id" element={<Registraion_Project_Edit />} />
              {/*fallback */}
              <Route path="*" element={<Dashboard />} />
            </Routes>
          </main>
        </div>
      </ThemeProvider>
    </ColorModeContext.Provider>
  );
}

export default App;
