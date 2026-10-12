import { Link } from "react-router";
import { ArrowLeft } from "lucide-react";
import { EmptyState } from "../components/ui";

export function NotFoundPage() {
  return (
    <section className="panel">
      <EmptyState
        title="Page not found"
        description="This page does not exist. Return to the dashboard to continue managing your campus."
        action={
          <Link className="button-primary" to="/">
            <ArrowLeft className="size-4" />
            Back to dashboard
          </Link>
        }
      />
    </section>
  );
}
