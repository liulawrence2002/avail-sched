import { useNavigate } from 'react-router-dom';
import Button from '../components/Button';
import EmptyState from '../components/EmptyState';

export default function NotFoundPage() {
  const navigate = useNavigate();

  return (
    <div className="min-h-[60vh] flex items-center justify-center px-4">
      <EmptyState
        icon="🦎"
        title="Lost in the goblin tunnels"
        description="The page you're looking for doesn't exist. Maybe a goblin misplaced it?"
        action={
          <div className="flex flex-col sm:flex-row gap-3">
            <Button variant="primary" onClick={() => navigate('/')}>
              Go Home
            </Button>
            <Button variant="ghost" onClick={() => navigate('/create')}>
              Create Event
            </Button>
          </div>
        }
      />
    </div>
  );
}
