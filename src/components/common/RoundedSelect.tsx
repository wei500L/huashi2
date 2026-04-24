import React from 'react';
import { CustomSelect } from './CustomSelect';

export type RoundedSelectOption = {
  value: string;
  label: string;
};

type RoundedSelectProps = {
  value: string;
  options: RoundedSelectOption[];
  placeholder?: string;
  className?: string;
  onChange: (value: string) => void;
};

export const RoundedSelect: React.FC<RoundedSelectProps> = ({
  value,
  options,
  placeholder = '--',
  className,
  onChange,
}) => <CustomSelect value={value} options={options} placeholder={placeholder} className={className} onChange={onChange} />;
