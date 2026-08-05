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
  disabled?: boolean;
  loading?: boolean;
  validationState?: 'default' | 'invalid' | 'success';
  id?: string;
  ariaLabel?: string;
  ariaDescribedBy?: string;
  onChange: (value: string) => void;
};

export const RoundedSelect: React.FC<RoundedSelectProps> = ({
  value,
  options,
  placeholder = '--',
  className,
  disabled,
  loading,
  validationState,
  id,
  ariaLabel,
  ariaDescribedBy,
  onChange,
}) => (
  <CustomSelect
    id={id}
    value={value}
    options={options}
    placeholder={placeholder}
    className={className}
    disabled={disabled}
    loading={loading}
    validationState={validationState}
    ariaLabel={ariaLabel}
    ariaDescribedBy={ariaDescribedBy}
    onChange={onChange}
  />
);
