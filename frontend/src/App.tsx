import { Route, Routes } from "react-router";
import { NotificationProvider } from "./components/NotificationProvider";
import { AppLayout } from "./layouts/AppLayout";
import { DashboardPage } from "./pages/DashboardPage";
import { ResourcePage } from "./pages/ResourcePage";
import { ResourceDetailsPage } from "./pages/ResourceDetailsPage";
import { EnrollmentsPage } from "./pages/EnrollmentsPage";
import { NotFoundPage } from "./pages/NotFoundPage";
import { courseConfig, studentConfig } from "./utils/resourceConfig";

export default function App() {
  return (
    <NotificationProvider>
      <Routes>
        <Route element={<AppLayout />}>
          <Route index element={<DashboardPage />} />
          <Route
            path="students"
            element={<ResourcePage key="students" config={studentConfig} />}
          />
          <Route
            path="students/:id"
            element={
              <ResourceDetailsPage
                key="student-details"
                config={studentConfig}
              />
            }
          />
          <Route
            path="courses"
            element={<ResourcePage key="courses" config={courseConfig} />}
          />
          <Route
            path="courses/:id"
            element={
              <ResourceDetailsPage key="course-details" config={courseConfig} />
            }
          />
          <Route path="enrollments" element={<EnrollmentsPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </NotificationProvider>
  );
}
