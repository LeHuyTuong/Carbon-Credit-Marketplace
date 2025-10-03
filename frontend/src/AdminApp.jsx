import { useState } from "react";
import { Routes, Route } from "react-router-dom";
import TopBar from "./pages/Dashboard/Admin/global/Topbar";
import Sidebar from "./pages/Dashboard/Admin/global/Sidebar";
import Dashboard from "./pages/Dashboard/Admin/dashboard";
import Team from "./pages/Dashboard/Admin/team";
import Invoices from "./pages/Dashboard/Admin/invoices";
import Contacts from "./pages/Dashboard/Admin/contacts";
import Bar from "./pages/Dashboard/Admin/bar";
import Form from "./pages/Dashboard/Admin/form";
import Line from "./pages/Dashboard/Admin/line";
import Pie from "./pages/Dashboard/Admin/pie";
import FAQ from "./pages/Dashboard/Admin/faq";
import Geography from "./pages/Dashboard/Admin/geography";
import { CssBaseline, ThemeProvider } from "@mui/material";
import { ColorModeContext, useMode } from "./theme";
import Calendar from "./pages/Dashboard/Admin/calendar/Calendar.jsx";
// 👇 import css admin riêng
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
              <Route path="/team" element={<Team />} />
              <Route path="/contacts" element={<Contacts />} />
              <Route path="/invoices" element={<Invoices />} />
              <Route path="/form" element={<Form />} />
              <Route path="/bar" element={<Bar />} />
              <Route path="/pie" element={<Pie />} />
              <Route path="/line" element={<Line />} />
              <Route path="/faq" element={<FAQ />} />
              <Route path="/calendar" element={<Calendar />} />
              <Route path="/geography" element={<Geography />} />
            </Routes>
          </main>
        </div>
      </ThemeProvider>
    </ColorModeContext.Provider>
  );
}

export default App;
