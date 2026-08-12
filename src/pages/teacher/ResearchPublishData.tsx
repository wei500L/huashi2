import React from 'react';
import { useParams } from 'react-router-dom';
import { ResearchWorkspace } from '@/features/research-analytics/components/ResearchWorkspace';

const ResearchPublishDataPage: React.FC = () => {
  const { publishId } = useParams();
  return (
    <div className="page-stack pb-16">
      <ResearchWorkspace initialPublishId={publishId ? Number(publishId) : null} />
    </div>
  );
};

export default ResearchPublishDataPage;
