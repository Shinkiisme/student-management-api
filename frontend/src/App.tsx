import { Route, Routes } from "react-router";
import { NotificationProvider } from "./components/NotificationProvider";
import { AuthProvider } from "./components/AuthProvider";
import {
  AdminRoute,
  AuthenticatedLayout,
  ProtectedRoute,
} from "./components/ProtectedRoute";
import { LoginPage } from "./pages/LoginPage";
import { DashboardPage } from "./pages/DashboardPage";
import { ResourcePage } from "./pages/ResourcePage";
import { ResourceDetailsPage } from "./pages/ResourceDetailsPage";
import { EnrollmentsPage } from "./pages/EnrollmentsPage";
import { NotFoundPage } from "./pages/NotFoundPage";
import { courseConfig, studentConfig } from "./utils/resourceConfig";

export default function App() {
  return (
    <AuthProvider>
      <NotificationProvider>
        <Routes>
          <Route path="login" element={<LoginPage />} />
          <Route element={<ProtectedRoute />}>
            <Route element={<AuthenticatedLayout />}>
              <Route element={<AdminRoute />}>
                <Route index element={<DashboardPage />} />
                <Route
                  path="students"
                  element={
                    <ResourcePage key="students" config={studentConfig} />
                  }
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
                <Route path="enrollments" element={<EnrollmentsPage />} />
              </Route>
              <Route
                path="courses"
                element={<ResourcePage key="courses" config={courseConfig} />}
              />
              <Route
                path="courses/:id"
                element={
                  <ResourceDetailsPage
                    key="course-details"
                    config={courseConfig}
                  />
                }
              />
              <Route path="*" element={<NotFoundPage />} />
            </Route>
          </Route>
        </Routes>
      </NotificationProvider>
    </AuthProvider>
  );
}
