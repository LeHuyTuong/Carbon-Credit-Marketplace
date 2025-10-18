import { useState } from "react";
import { Routes, Route } from "react-router-dom";
import TopBar from "./pages/Dashboard/Admin/global/Topbar";
import Sidebar from "./pages/Dashboard/Admin/global/Sidebar";
import Dashboard from "./pages/Dashboard/Admin/dashboard";
import User_Management from "./pages/Dashboard/Admin/User_Management/index.jsx";
import Transaction_Management from "./pages/Dashboard/Admin/Transaction_Management/index.jsx";
import Credit_Management from "./pages/Dashboard/Admin/Credit_Management/index.jsx";
import Report_Management from "./pages/Dashboard/Admin/Report_Management/index.jsx";
import EV_Management from "./pages/Dashboard/Admin/EV_Management/index.jsx";
import Project_Management from "./pages/Dashboard/Admin/Project_Management/index.jsx";
import View_Profile from "./pages/Dashboard/Admin/profile/view.jsx";
import Edit_Profile from "./pages/Dashboard/Admin/profile/edit.jsx";
import User_View from "./pages/Dashboard/Admin/User_Management/view.jsx";
import Transaction_View from "./pages/Dashboard/Admin/Transaction_Management/view.jsx";
import Credit_View from "./pages/Dashboard/Admin/Credit_Management/view.jsx";
import Report_View from "./pages/Dashboard/Admin/Report_Management/view.jsx";
import EV_View from "./pages/Dashboard/Admin/EV_Management/view.jsx";
import Project_View from "./pages/Dashboard/Admin/Project_Management/view.jsx";
import New_Project from "./pages/Dashboard/Admin/Project_Management/newProject.jsx";
import Company_Management from "./pages/Dashboard/Admin/Company_Management/index.jsx";
import Company_View from "./pages/Dashboard/Admin/Company_Management/view.jsx";
import Company_Edit from "./pages/Dashboard/Admin/Company_Management/edit.jsx";
import Bar from "./pages/Dashboard/Admin/bar";
import Form from "./pages/Dashboard/Admin/form";
import Line from "./pages/Dashboard/Admin/line";
import Pie from "./pages/Dashboard/Admin/pie";
import FAQ from "./pages/Dashboard/Admin/faq";
import Geography from "./pages/Dashboard/Admin/geography";
import { CssBaseline, ThemeProvider } from "@mui/material";
import { ColorModeContext, useMode } from "./theme";
import Calendar from "./pages/Dashboard/Admin/calendar/Calendar.jsx";
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
              <Route path="/user_management" element={<User_Management />} />
              <Route path="/credit_management" element={<Credit_Management />} />
              <Route path="/transaction_management" element={<Transaction_Management />} />
              <Route path="/report_management" element={<Report_Management />} />
              <Route path="/ev_management" element={<EV_Management />} />
              <Route path="/project_management" element={<Project_Management />} />
              <Route path="/form" element={<Form />} />
              <Route path="/bar" element={<Bar />} />
              <Route path="/pie" element={<Pie />} />
              <Route path="/line" element={<Line />} />
              <Route path="/faq" element={<FAQ />} />
              <Route path="/calendar" element={<Calendar />} />
              <Route path="/geography" element={<Geography />} />
              <Route path="/view_profile_admin" element={<View_Profile />} />
              <Route path="/edit_profile_admin" element={<Edit_Profile />} />
              <Route path="/view_user/:id" element={<User_View />} />
              <Route path="/view_credit/:id" element={<Credit_View />} />
              <Route path="/view_transaction/:id" element={<Transaction_View />} />
              <Route path="/view_report/:id" element={<Report_View />} />
              <Route path="/view_EV/:id" element={<EV_View />} />
              <Route path="/view_project/:id" element={<Project_View />} />
              <Route path="/new_project" element={<New_Project />} />
              <Route path="/company_management" element={<Company_Management />} />
              <Route path="/view_company/:id" element={<Company_View />} />
              <Route path="/edit_company/:id" element={<Company_Edit />} />
              <Route path="*" element={<Dashboard />} />
            </Routes>
          </main>
        </div>
      </ThemeProvider>
    </ColorModeContext.Provider>
  );
}

export default App;
