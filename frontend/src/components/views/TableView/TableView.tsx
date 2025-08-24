import React from 'react';
import { Table, Tag, Space, Button, Tooltip } from 'antd';
import { EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { useModel } from '../../../contexts/ModelContext';

const TableView: React.FC = () => {
  const { requirements, selectedId, selectElement, deleteRequirement } = useModel();

  const requirementColumns = [
    {
      title: 'ID',
      dataIndex: 'reqId',
      key: 'reqId',
      width: 120,
    },
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      width: 200,
    },
    {
      title: '描述',
      dataIndex: 'text',
      key: 'text',
      ellipsis: {
        showTitle: false,
      },
      render: (text: string) => (
        <Tooltip placement="topLeft" title={text}>
          {text}
        </Tooltip>
      ),
    },
    {
      title: '标签',
      dataIndex: 'tags',
      key: 'tags',
      render: (tags: string[]) => (
        <>
          {tags?.map(tag => (
            <Tag color="blue" key={tag}>
              {tag}
            </Tag>
          ))}
        </>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_: any, record: any) => (
        <Space size="middle">
          <Button 
            type="link" 
            icon={<EditOutlined />} 
            size="small"
            onClick={() => selectElement(record.id)}
          />
          <Button 
            type="link" 
            icon={<DeleteOutlined />} 
            size="small"
            danger
            onClick={() => deleteRequirement(record.id)}
          />
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: '16px' }}>
      <h3>需求定义表格视图</h3>
      <Table 
        columns={requirementColumns}
        dataSource={requirements}
        rowKey="id"
        size="small"
        locale={{
          emptyText: '暂无数据'
        }}
        pagination={{
          pageSize: 10,
          showSizeChanger: true,
          showQuickJumper: true,
        }}
        onRow={(record) => {
          return {
            onClick: () => selectElement(record.id),
            style: {
              backgroundColor: selectedId === record.id ? '#e6f7ff' : undefined,
            }
          };
        }}
      />
    </div>
  );
};

export default TableView;